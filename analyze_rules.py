# -*- coding: utf-8 -*-
"""分析规则文档结构，评估 DocumentByParagraphSplitter(1600, 200) 切分策略"""
import os, re, statistics

RULES_DIR = r"D:\learn\Myjava\JUC\task3\aftersight\src\main\resources\rules"

def read_docs():
    docs = {}
    for fn in sorted(os.listdir(RULES_DIR)):
        if fn.endswith('.md'):
            with open(os.path.join(RULES_DIR, fn), encoding='utf-8') as f:
                docs[fn] = f.read()
    return docs

docs = read_docs()

# 1. 总字符数
print("=== 1. 文件总字符数 ===")
total = 0
for fn, text in docs.items():
    clean = re.sub(r'\s', '', text)
    total += len(clean)
    print(f"{fn}: 原始 {len(text)} / 去空白 {len(clean)}")
print(f"全部文档去空白字符合计: {total}")

# 2. 按 ### 条款块统计
print("\n=== 2. ### 条款块长度分布（含标题，不含下级 ## 内容）===")
all_clause = []
for fn, text in docs.items():
    parts = re.split(r'(?m)^###\s', text)
    for p in parts[1:]:
        m = re.match(r'([^\n]+)\n(.*)', p, re.S)
        title = m.group(1).strip()
        content = re.split(r'(?m)^##\s', m.group(2))[0].strip()
        length = len(content)
        all_clause.append((fn, title, length))
        if length > 900:
            print(f"  超长条款 [{fn}] {title}: {length} 字符")
lens = [c[2] for c in all_clause]
print(f"条款块数量: {len(lens)}")
print(f"长度: min={min(lens)}, max={max(lens)}, 平均={statistics.mean(lens):.0f}, 中位数={statistics.median(lens):.0f}")
for th in (500, 800, 1200, 1600):
    print(f"  超过{th}字符: {sum(1 for l in lens if l > th)} 条")

# 3. ## 大类块长度
print("\n=== 3. ## 大类块长度分布 ===")
all_section = []
for fn, text in docs.items():
    parts = re.split(r'(?m)^##\s', text)
    for p in parts[1:]:
        m = re.match(r'([^\n]+)\n(.*)', p, re.S)
        title = m.group(1).strip()
        content = re.split(r'(?m)^#\s', m.group(2))[0].strip()
        all_section.append((fn, title, len(content)))
lens2 = [s[2] for s in all_section]
print(f"大类块数量: {len(lens2)}")
print(f"长度: min={min(lens2)}, max={max(lens2)}, 平均={statistics.mean(lens2):.0f}, 中位数={statistics.median(lens2):.0f}")

# 4. 段落长度（空行分隔）
print("\n=== 4. 段落长度分布（空行分隔）===")
all_para = []
for fn, text in docs.items():
    for p in re.split(r'\n\s*\n', text):
        p = p.strip()
        if p:
            all_para.append(len(p))
lens3 = all_para
print(f"段落数量: {len(lens3)}")
print(f"长度: min={min(lens3)}, max={max(lens3)}, 平均={statistics.mean(lens3):.0f}, 中位数={statistics.median(lens3):.0f}")
for th in (800, 1200, 1600):
    print(f"  超过{th}字符: {sum(1 for l in lens3 if l > th)} 段")

# 5. 模拟 DocumentByParagraphSplitter(1600, 200)
print("\n=== 5. 模拟切分 1600/200（段落按空行，贪心合并 + 尾部重叠）===")
def simulate(text, max_seg, overlap):
    paras = [p.strip() for p in re.split(r'\n\s*\n', text) if p.strip()]
    segs = []
    cur = ""
    for para in paras:
        if not cur:
            cur = para
            continue
        if len(cur) + 1 + len(para) <= max_seg:
            cur += "\n" + para
        else:
            tail = cur[-overlap:] if len(cur) >= overlap else cur
            segs.append(cur)
            cur = tail + "\n" + para
    if cur:
        segs.append(cur)
    return segs

for ms, ov in [(1600, 200), (800, 100), (500, 100)]:
    total_segs = 0
    print(f"--- 参数 maxSegmentSize={ms}, overlap={ov} ---")
    for fn, text in docs.items():
        segs = simulate(text, ms, ov)
        total_segs += len(segs)
    print(f"全部文档切片数: {total_segs}")

# 6. 条款跨切片检查（1600/200 下，### 条款是否被切边界切断）
print("\n=== 6. 条款跨切片检查（1600/200）===")
def build_clause_spans(text):
    """返回 [(title, start, end)] 按 ### 分割，用绝对位置"""
    spans = []
    # 用正则找所有 ### 标题位置
    matches = list(re.finditer(r'(?m)^###\s', text))
    for i, m in enumerate(matches):
        start = m.start()
        end = matches[i+1].start() if i+1 < len(matches) else len(text)
        title = text[m.end():].split('\n', 1)[0].strip()
        spans.append((title, start, end))
    return spans

def seg_boundaries(text, max_seg=1600, overlap=200):
    """返回切片边界位置列表（切片结束位置）"""
    paras = [p.strip() for p in re.split(r'\n\s*\n', text) if p.strip()]
    # 记录每个段落在原文的位置比较麻烦，简化：用累积长度近似
    pos = 0
    bounds = []
    cur_len = 0
    for i, para in enumerate(paras):
        if cur_len == 0:
            cur_len = len(para)
            pos = len(para)
            continue
        if cur_len + 1 + len(para) <= max_seg:
            cur_len += 1 + len(para)
            pos += 1 + len(para)
        else:
            bounds.append(pos)
            # 新段从重叠开始：长度=overlap + len(para)
            cur_len = min(overlap, len(para)) + 1 + len(para)
            pos += 1 + len(para)
    return bounds

cross_count = 0
total_clause = 0
for fn, text in docs.items():
    spans = build_clause_spans(text)
    bounds = seg_boundaries(text)
    total_clause += len(spans)
    for title, s, e in spans:
        # 若某个切片边界落在条款内部（s < bound < e），条款被切断
        for b in bounds:
            if s < b < e:
                cross_count += 1
                if cross_count <= 10:
                    print(f"  被切断 [{fn}] {title}（条款 {s}-{e}，切点 {b}）")
                break
print(f"被切断条款数: {cross_count} / 总条款 {total_clause}")
