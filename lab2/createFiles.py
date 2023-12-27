import re
import os

data_path = './document.dat'
output_dir = './Files'
# 读取document.dat文件
with open(data_path, 'r', encoding='gb18030') as file:
    content = file.read()

# 使用正则表达式提取document.dat中的内容
files = re.findall(r'<doc>.*?</doc>', content, re.DOTALL)
if not os.path.exists(output_dir):
    os.makedirs(output_dir)

for txt in files:
    # 提取title和text
    title_detail = re.search(r'<contenttitle>(.*?)</contenttitle>', txt)
    content_detail = re.search(r'<content>(.*?)</content>', txt, re.DOTALL)
    title = title_detail.group(1)
    content = content_detail.group(1)
    # 将title和text写入文件
    output = title
    output += content

    with open(os.path.join(output_dir, f'{title}.txt'), 'w', encoding='utf-8') as file:
        file.write(output)
print("Files are created successfully!")
