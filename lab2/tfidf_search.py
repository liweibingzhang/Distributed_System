import re
from collections import defaultdict
# 创建一个 defaultdict，用于存储词与文件及其对应的 TF-IDF 值的映射关系
word_dict = defaultdict(dict)

# 正则表达式模式，用于匹配每一行中的文件名、内容和 TF-IDF 值
filename_equal = re.compile(r'\[([^,[]+),')
content_equal = re.compile(r'([\S]+)\s')
value_equal = re.compile(r',(\d+\.\d+)')

with open("./Document.txt", "r", encoding="utf-8") as Doc:
    while 1:
        text = Doc.readline()
        if text == "" or text is None:
            break
        # 使用正则表达式匹配词、文件名和 TF-IDF 值
        filename = re.findall(filename_equal, text)
        content = re.match(content_equal, text).group(1)
        TF_value = re.findall(value_equal, text)
        # 将 TF-IDF 值转换为浮点数类型
        TF_value = [float(val) for val in TF_value]
        # 创建一个字典，将文件名和对应的 TF-IDF 值进行映射，并将该字典添加到 word_map 中
        file_map = dict(zip(filename, TF_value))
        word_dict[content] = file_map
while 1:
    print("Please input keyword: ", end="")
    key = input()
    if key in word_dict:
        # 打印该关键词对应的文件名
        for title in word_dict[key].keys():
            print(title)
    else:
        print("Keyword doesn't exist!")
