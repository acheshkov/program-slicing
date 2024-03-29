from functools import reduce
from typing import Set
from unittest import TestCase, main

from program_graphs.adg.parser.java.parser import parse  # type: ignore

from slicing.block.block import gen_block_slices
from slicing.block.block import get_entry_candidates, get_node_lines, mk_declared_variables_table
from slicing.block.filters import at_least_one_block_stmt, last_or_next_statement_is_control
from slicing.block.state import State
from slicing.block.utils import get_occupied_line_range, count_ncss, find_blank_and_full_comment_lines


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
        state = State(adg, None, None, {}, {}, {})  # type: ignore
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
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
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
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
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
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1, 2, 3, 4], bss)
        self.assertIn([2, 3, 4], bss)
        self.assertIn([3, 4], bss)
        self.assertIn([1], bss)
        self.assertIn([2], bss)
        self.assertIn([3], bss)
        self.assertIn([4], bss)
        self.assertIn([1, 2, 3], bss)
        self.assertNotIn([1, 2], bss)

    def test_block_slice_class_field_not_need_to_return(self) -> None:
        code = """
            int a = 1;
            int d = 1;
            a = a + 1;
            b = a + d;
            int c = a + b;
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([2, 3, 4], bss)

    def test_mk_declared_variables_table(self) -> None:
        code = """
            T o;
            int a = 1;
            a = a + 1;
            o[0] = 1;
            int c = a + o[0];
        """
        adg = parse(code)
        t = mk_declared_variables_table(adg.to_ddg(), code)
        self.assertIn(set(['a']), t.values())
        self.assertIn(set(['o']), t.values())
        self.assertIn(set(['c']), t.values())

    def test_block_slice_readonly_object_not_need_to_return(self) -> None:
        code = """
            T o;
            int a = 1;
            int b = 1;
            a = a + b;
            o[0] = 1;
            int c = a + o[0];
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([3, 4, 5], bss)

    def test_block_slice_no_repetitions(self) -> None:
        code = """
            int a = 1;
            for (;;){
                stmt();
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1], bss)
        self.assertIn([3], bss)
        self.assertIn([2, 3, 4], bss)
        self.assertIn([1, 2, 3, 4], bss)
        self.assertEqual(len(bss), 4)

    def test_block_slice_bad_format(self) -> None:
        code = """
            int a = 1;
            for (;;)
            {
                stmt();
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1], bss)
        self.assertIn([4], bss)
        self.assertIn([2, 3, 4, 5], bss)
        self.assertIn([1, 2, 3, 4, 5], bss)
        self.assertEqual(len(bss), 4)

    def test_block_slice_var_declaration(self) -> None:
        code = """
            int a = 1;
            int b = 1;
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1], bss)
        self.assertIn([2], bss)
        self.assertIn([1, 2], bss)

    def test_block_slice_if_else_if_first_line_shared(self) -> None:
        code = """
            if () {
            } else if (){
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1, 2, 3], bss)
        self.assertNotIn([2, 3], bss)

    def test_block_slice_if_else_if_new_line(self) -> None:
        code = """
            if () {
            } else
                if (){
                }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1, 2, 3, 4], bss)
        self.assertIn([3, 4], bss)

    def test_block_slice_at_least_one_block_stmt(self) -> None:
        code = """
            stmt;
            if (){ }
            stmt;
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code, [at_least_one_block_stmt])]
        self.assertIn([1, 2], bss)
        self.assertIn([2, 3], bss)
        self.assertIn([1, 2, 3], bss)
        self.assertIn([2], bss)
        self.assertNotIn([1], bss)
        self.assertNotIn([3], bss)

    def test_block_slice_last_ast_statement_filter(self) -> None:
        code = """
            stmt;
            stmt;
            if (){ }
            stmt;
            stmt;
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in
               gen_block_slices(adg, code, [last_or_next_statement_is_control])]
        self.assertIn([1, 2], bss)
        self.assertIn([1, 2, 3], bss)
        self.assertIn([1, 2, 3, 4, 5], bss)
        self.assertIn([3], bss)
        self.assertIn([4, 5], bss)
        self.assertIn([3, 4, 5], bss)
        self.assertNotIn([1], bss)
        self.assertNotIn([3, 4], bss)

    def test_block_slice_try_catch(self) -> None:
        code = """
            try {
                stmt;
            }  catch (Exception e) {
                stmt;
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1, 2, 3, 4, 5], bss)
        self.assertIn([2], bss)
        self.assertIn([4], bss)
        self.assertNotIn([2, 3, 4, 5], bss)

    def test_block_slice_try_finally(self) -> None:
        code = """
            try(T a = new T();) {
                stmt();
            }
            finally {
                stmt();
            }
            stmt();
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertNotIn([5, 6, 7], bss)
        self.assertNotIn([2, 3, 4, 5, 6], bss)
        self.assertNotIn([2, 3, 4, 5, 6, 7], bss)

    def test_block_slice_try_with_resources(self) -> None:
        code = """
            try (T a = new T();) {
                stmt;
            }
            if () {
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([2], bss)
        self.assertIn([4, 5], bss)
        self.assertIn([1, 2, 3], bss)
        self.assertIn([1, 2, 3, 4, 5], bss)
        self.assertNotIn([2, 3, 4, 5], bss)

    def test_block_slice_for(self) -> None:
        code = """
            for (int i = 0; i < 10; i++){
                stmt();
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1, 2, 3], bss)
        self.assertIn([2], bss)
        self.assertNotIn([1, 2], bss)

    def test_block_slice_for_last_line_shared(self) -> None:
        code = """
            for (int i = 0; i < 10; i++){
                stmt();
            } int x =
                y + 4;
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertNotIn([1, 2, 3], bss)
        self.assertIn([1, 2, 3, 4], bss)
        self.assertIn([2], bss)
        self.assertNotIn([3, 4], bss)

    def test_block_slice_for_update_clause_not_included(self) -> None:
        code = """
            stmt;
            for (expr;expr;stmt){
                stmt();
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1], bss)
        self.assertIn([3], bss)
        self.assertIn([2, 3, 4], bss)
        self.assertIn([1, 2, 3, 4], bss)
        self.assertNotIn([2, 3], bss)
        self.assertNotIn([3, 4], bss)

    def test_block_slice_complete_return(self) -> None:
        code = """
            stmt;
            if (){
                return;
            } else {
                return;
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1], bss)
        self.assertIn([3], bss)
        self.assertIn([5], bss)
        self.assertIn([1, 2, 3, 4, 5, 6], bss)
        self.assertIn([2, 3, 4, 5, 6], bss)

    def test_block_slice_complete_return_and_throw(self) -> None:
        code = """
            stmt;
            if (){
                return;
            } else {
                throw;
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1], bss)
        self.assertIn([3], bss)
        self.assertIn([1, 2, 3, 4, 5, 6], bss)
        self.assertIn([2, 3, 4, 5, 6], bss)

    def test_block_slice_non_complete_return(self) -> None:
        code = """
            stmt;
            if (){
                return;
            } else {
                stmt;
            }
            stmt;
            stmt;
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([1], bss)
        self.assertIn([3], bss)
        self.assertIn([5], bss)
        self.assertIn([7], bss)
        self.assertIn([7, 8], bss)
        self.assertNotIn([1, 2, 3, 4, 5, 6], bss)
        self.assertNotIn([1, 2, 3, 4, 5, 6, 7], bss)
        self.assertIn([2, 3, 4, 5, 6, 7, 8], bss)
        self.assertIn([1, 2, 3, 4, 5, 6, 7, 8], bss)

    def test_block_slice_bug(self) -> None:
        code = """
            A a = new A();
            B b = new B();
            a == 1;
            b == 1;
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertNotIn([1, 2], bss)

    def test_block_slice_non_complete_return_but_there_are_no_statement_after(self) -> None:
        code = """
            if () {
                syncronized (a) {
                    if (){
                        return;
                    } else {
                        stmt;
                    }
                }
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertIn([3, 4, 5, 6, 7], bss, msg='we can extract this block because there are no more statement after')

    def test_count_ncss(self) -> None:
        comment_lines = count_ncss(((0, 0), (9, 0)), {2, 3, 4})
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
        self.assertEqual(r, ((1, 8), (5, 9)))

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
        self.assertEqual(r, ((1, 8), (9, 9)))

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
        self.assertEqual(r, ((1, 8), (9, 9)))

    def test_ncss_whole_snippet_with_multiline_statement(self) -> None:
        code = """
        if (){
            a = b +
                c;
        }"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, ((1, 8), (4, 9)))

    def test_get_occupied_line_range_multiline_single_statement(self) -> None:
        code = """
        fun(
            a,
            b,
            c
        );"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, ((1, 8), (5, 10)))

    def test_get_occupied_line_range_with_one_line_multiple_stmts(self) -> None:
        code = """
        if (){
            int a = 4; stmt();
        }"""
        adg = parse(code)
        r = get_occupied_line_range(adg.to_ast(), adg.get_entry_node())
        self.assertEqual(r, ((1, 8), (3, 9)))

    def test_get_occupied_line_range(self) -> None:
        code = """
        if (){
            int a = 4;
        }
        """
        adg = parse(code)
        [if_node] = [node for node, name in adg.nodes(data='name') if name == 'if']
        r = get_occupied_line_range(adg.to_ast(), if_node)
        self.assertEqual(r, ((1, 8), (3, 9)))

    def test_get_occupied_line_range_no_brackets(self) -> None:
        code = """
        if ()
            int a = 4;

        """
        adg = parse(code)
        [if_node] = [node for node, name in adg.nodes(data='name') if name == 'if']
        r = get_occupied_line_range(adg.to_ast(), if_node)
        self.assertEqual(r, ((1, 8), (2, 22)))

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
        self.assertEqual(r, ((1, 8), (4, 20)))

    def test_duplicated_slices_enhanced_for(self) -> None:
        code = """
            for (T a : A) {
                    stmt;
            }
        """
        adg = parse(code)
        bss = [sorted(bs.block_slice_lines()) for bs in gen_block_slices(adg, code)]
        self.assertCountEqual([[1, 2, 3], [2]], bss)


if __name__ == '__main__':
    main()
