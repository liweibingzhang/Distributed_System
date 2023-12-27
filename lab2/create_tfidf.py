import sys
import jieba
from mrjob.job import MRJob
import re
import os
import io
from collections import defaultdict
import math

os.environ["PYTHONIOENCODING"] = "utf-8"


# 定义一个 MRJob 类，用于计算每个文档的词数
class Total_Word(MRJob):
    def mapper(self, _, line):
        file_path = os.environ["map_input_file"][7:-4]
        line = re.sub(r'\W*', "", line)
        word = jieba.cut_for_search(line)
        yield file_path, len(set(word))

    def reducer(self, key, values):
        yield key, sum(values)


# 定义一个 MRJob 类，用于计算每个文档中每个词的出现次数
class WordsCountPerFile(MRJob):

    def mapper(self, _, line):
        file_path = os.environ["map_input_file"][7:-4]
        line = re.sub(r"\W*", "", line)
        word = jieba.cut_for_search(line)

        for ch in set(word):
            yield f"{file_path},{ch}", 1

    def reducer(self, key, values):
        yield key, sum(values)


# 从字节流中获取文件名和词数并返回一个字典
def get_file_and_word_cnt_map(bytes_stream):
    content = bytes_stream.getvalue().decode("utf-8")
    tmp_map = {}
    lines = content.split("\n")

    # 定义正则表达式，用于匹配文件名和词数
    file_equal = re.compile(r'\\\\([^,"]+)"')
    cnt_equal = re.compile(r'\t(\d+)')

    for line in lines:
        if line == "":
            continue
        # 使用正则表达式匹配文件名和词数
        file = re.search(file_equal, line).group(1)
        cnt = int(re.search(cnt_equal, line).group(1))
        tmp_map[file] = cnt

    return tmp_map


# 计算文档中某个词出现的频率
def calculate_tf(total_map, single_map):
    for file, word_map in single_map.items():
        for word in word_map:
            single_map[file][word] /= total_map[file]

    return single_map


# 计算逆文档频率（IDF）并更新每个单词的出现次数。
def calculate_idf(word_map, file_cnt):
    for word, cnt in word_map.items():
        word_map[word] = math.log(file_cnt / (cnt + 1))

    return word_map


# 计算目录下的文件总数
def calculate_files(directory):
    # 获取指定目录下的所有文件
    files = [fo for fo in os.listdir(directory) if os.path.isfile(os.path.join(directory, fo))]

    total_files = len(files)

    return total_files


# 每个词在文档中出现的次数
def calculate_word_freq_in_file(word_cnt_in_map):
    total_word_map = defaultdict(int)
    for file, word_map in word_cnt_in_map.items():
        for word, cnt in word_map.items():
            if cnt > 0:
                total_word_map[word] += 1
    return total_word_map


# 从字节流中提取每个文档中单词的计数信息，并返回一个嵌套字典
def calculate_word_cnt_from_byte(bytes_stream):
    string = bytes_stream.getvalue().decode("utf-8")
    tmp_map = {}
    lines = string.split("\n")

    file_equal = re.compile(r'\\\\([^,"]+),')
    word_equal = re.compile(r',([^"]+)"')
    cnt_equal = re.compile(r'\t(\d+)')

    for line in lines:
        if line == "":
            continue
        file_res = re.search(file_equal, line)
        word_res = re.search(word_equal, line)
        cnt_res = re.search(cnt_equal, line)

        file = file_res.group(1)
        word = word_res.group(1)
        cnt = int(cnt_res.group(1))
        if file not in tmp_map:
            tmp_map[file] = {}
        tmp_map[file][word] = cnt

    return tmp_map


# 计算TF-IDF
def calculate_tf_idf(TF_Map, IDF_Map):
    for file, word_map in TF_Map.items():
        for word, tf in word_map.items():
            TF_Map[file][word] *= IDF_Map[word]

    return TF_Map


class Func_between_file_and_tf_idf:
    def __init__(self, filepath, tf_idf):
        self.filepath = filepath
        self.tf_idf = tf_idf

    def __str__(self):
        res = self.filepath.encode("utf-8").decode('unicode_escape')
        return f"{res},{self.tf_idf}"


# 根据每个单词的出现次数和 TF-IDF 映射构建输出映射。
def create_map(word_map, tf_idf_map):
    map_res = defaultdict(list)
    for word in word_map.keys():
        for file, word_map in tf_idf_map.items():
            if word in word_map:
                content = Func_between_file_and_tf_idf(file, word_map[word])
                map_res[word].append(content)

    return map_res


# 对输出映射中的内容进行排序
def sort_map_res(map_res):
    sort_map = defaultdict(list)
    for word, file in map_res.items():
        sort_map[word] = sorted(file, key=lambda res: res.tf_idf, reverse=True)
    return sort_map


tmp_out = sys.stdout

files_cnt = calculate_files("./Files")

# 保存总词数信息的字节流
total_word_num = io.BytesIO()
sys.stdout = total_word_num
Total_Word.run()
word_file_map = get_file_and_word_cnt_map(total_word_num)

# 保存单词出现次数信息
word_cnt = io.BytesIO()
sys.stdout = word_cnt
WordsCountPerFile.run()
single_word_count_of_doc_map = calculate_word_cnt_from_byte(word_cnt)

# 计算 TF 值
tf_map = calculate_tf(word_file_map, single_word_count_of_doc_map)
sys.stdout = tmp_out

# 计算单词出现次数
word_map = calculate_word_freq_in_file(single_word_count_of_doc_map)

# 计算IDF 值
idf_freq = calculate_idf(word_map, files_cnt)

# 计算 TF-IDF 值
tf_idf_val = calculate_tf_idf(tf_map, idf_freq)

# 构建输出映射
map_res = create_map(word_map, tf_idf_val)

# 对输出映射中的内容进行排序
map_res = sort_map_res(map_res)

with open("Document.txt", "w", encoding="utf-8") as Doc_file:
    for key, val in map_res.items():
        Doc_file.write(key.encode("utf-8").decode('unicode_escape') + "\t[")
        for i in range(len(val)):
            content = val[i]
            Doc_file.write("[")
            if i == len(val) - 1:
                Doc_file.write(content.__str__() + "]")
            else:
                Doc_file.write(content.__str__() + "],")
        Doc_file.write("]\n")
