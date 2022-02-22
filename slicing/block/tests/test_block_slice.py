
from unittest import TestCase, main
from program_graphs.adg.parser.java.parser import parse  # type: ignore
from slicing.block.block import gen_block_slices, get_entry_candidates, get_node_lines, block_slice_lines, State
from slicing.block.block import get_occupied_line_range, count_ncss, find_blank_and_full_comment_lines
from functools import reduce
from typing import Set


class TestBlockSlice(TestCase):

    def test_entry_candidates(self) -> None:
        code = """
            stmt();
            if (){
                stmt();
                stmt();
            }
            for (;;){
                stmt();
                stmt();
            }
            stmt()
        """
        adg = parse(code)
        state = State(adg)
        entry_lines: Set[int] = reduce(
            lambda a, b: a | b,
            [get_node_lines(adg, n) for n in get_entry_candidates(state)],
            set([])
        )
        self.assertSetEqual(entry_lines, {1, 2, 3, 6, 7, 10})

    def test_block_slice_control_flow(self) -> None:
        code = """
            if (){
                stmt();
                stmt();
            }
            stmt();
        """
        adg = parse(code)
        bss = [sorted(block_slice_lines(adg, bs)) for bs in gen_block_slices(adg)]
        self.assertIn([1, 2, 3, 4], bss)
        self.assertIn([1, 2, 3, 4, 5], bss)
        self.assertIn([2], bss)
        self.assertIn([2, 3], bss)
        self.assertIn([5], bss)
        self.assertNotIn([3], bss)
        self.assertNotIn([1, 2], bss)
        self.assertNotIn([1, 2, 3], bss)
        self.assertNotIn([2, 3, 4], bss)
        self.assertNotIn([2, 3, 5], bss)

    def test_block_slice_control_flow_if_else(self) -> None:
        code = """
            if (x == 0) {
                stmt();
            } else {
                stmt();
            }
        """
        adg = parse(code)
        bss = [sorted(block_slice_lines(adg, bs)) for bs in gen_block_slices(adg)]
        self.assertIn([1, 2, 3, 4, 5], bss)
        self.assertIn([2], bss)
        self.assertIn([4], bss)
        self.assertNotIn([1, 2], bss)
        self.assertNotIn([3], bss)
        self.assertNotIn([4, 5], bss)

    def test_block_slice_data_dependency(self) -> None:
        code = """
            int a = 1;
            int b = a;
            int c = a + b;
            int d = c;
        """
        adg = parse(code)
        bss = [sorted(block_slice_lines(adg, bs)) for bs in gen_block_slices(adg)]
        self.assertIn([1], bss)
        self.assertIn([1, 2, 3], bss)
        self.assertIn([1, 2, 3, 4], bss)
        self.assertNotIn([1, 2], bss)

    def test_block_slice_no_repetitions(self) -> None:
        code = """
            int a = 1;
            for (;;){
                stmt();
            }
        """
        adg = parse(code)
        bss = [sorted(block_slice_lines(adg, bs)) for bs in gen_block_slices(adg)]
        self.assertIn([1], bss)
        self.assertIn([3], bss)
        self.assertIn([2, 3, 4], bss)
        self.assertIn([1, 2, 3, 4], bss)
        self.assertEqual(len(bss), 4)

    def test_count_ncss(self) -> None:
        comment_lines = count_ncss((0, 9), {2, 3, 4})
        self.assertEqual(comment_lines, 7)

    def test_find_comment_lines(self) -> None:
        code = """
            // comment

            if (){
                // comment
                stmt(); // comment
                a = b +
                // comment
                    + c;

            } else {
                 stmt();
            }"""
        adg = parse(code)
        comment_lines = find_blank_and_full_comment_lines(adg.to_ast(), adg.get_entry_node())
        self.assertSetEqual(comment_lines, {1, 2, 4, 7, 9})

    def test_get_occupied_line_range_whole_snippet(self) -> None:
        code = """
            if (){
                stmt();
            } else {
                 stmt();
            }"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, (1, 5))

    def test_get_occupied_line_range_whole_snippet_long(self) -> None:
        code = """
            if
            ()
            {
                stmt();
            }
            else
            {
                stmt();
            }"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, (1, 9))

    def test_get_occupied_line_range_whole_snippet_with_comments(self) -> None:
        code = """
            // comment
            if (){
                /*
                    Multiline comment
                */
                stmt();
            } else {
                 stmt();
            }"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, (1, 9))

    def test_ncss_whole_snippet_with_multiline_statement(self) -> None:
        code = """
            if (){
                a = b +
                    c;
            }"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, (1, 4))

    def test_get_occupied_line_range_multiline_single_statement(self) -> None:
        code = """
            fun(
                a,
                b,
                c
            );"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, (1, 5))

    def test_get_occupied_line_range_with_one_line_multiple_stmts(self) -> None:
        code = """
            if (){
                int a = 4; stmt();
            }"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, (1, 3))

    def test_get_occupied_line_range(self) -> None:
        code = """
            if (){
                int a = 4;
            }
        """
        adg = parse(code)
        [if_node] = [node for node, name in adg.nodes(data='name') if name == 'if']
        r = get_occupied_line_range(adg.to_ast(), if_node)
        self.assertEqual(r, (1, 3))

    def test_get_occupied_line_range_no_brackets(self) -> None:
        code = """
            if ()
                int a = 4;

        """
        adg = parse(code)
        [if_node] = [node for node, name in adg.nodes(data='name') if name == 'if']
        r = get_occupied_line_range(adg.to_ast(), if_node)
        self.assertEqual(r, (1, 2))

    def test_get_occupied_line_range_bad_format(self) -> None:
        code = """
            if ()
            {
                int a = 4;
                stmt();} stmt();
        """
        adg = parse(code)
        [if_node] = [node for node, name in adg.nodes(data='name') if name == 'if']
        r = get_occupied_line_range(adg.to_ast(), if_node)
        self.assertEqual(r, (1, 4))


if __name__ == '__main__':
    main()