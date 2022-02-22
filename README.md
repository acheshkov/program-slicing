# program-slicing


```python
from program_graphs.adg import parse_java
from slicing.block.block import gen_block_slices, mk_max_ncss_filter

java_code = '''
    int a = 1;
    for (;;){
        stmt();
    }
'''

adg = parse_java(java_code)
for _, _, _, line_range, _ in gen_block_slices(adg):
    print(line_range)
```

Expected output (up to ordering):
```bash
(1, 1)
(3, 3)
(1, 4)
(2, 4)
```