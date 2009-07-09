package net.sourceforge.vrapper.platform;

import net.sourceforge.vrapper.utils.LineInformation;
import net.sourceforge.vrapper.utils.Position;
import net.sourceforge.vrapper.utils.Search;
import net.sourceforge.vrapper.utils.SearchResult;
import net.sourceforge.vrapper.utils.Space;

/**
 * Common interface for model and view space to implement.
 *
 * @author Krzysiek Goj
 */
public interface TextContent {

    /**
     * @param line
     *            a line in the text, zero-based.
     * @return the line information of the specified line.
     */
    LineInformation getLineInformation(int line);

    /**
     * @param offset
     *            a position in the text, zero-based.
     * @return the line information of the line which contains the given
     *         position.
     */
    LineInformation getLineInformationOfOffset(int offset);

    /**
     * @return number of lines in the text.
     */
    int getNumberOfLines();

    /**
     * @param index
     *            start of the text to replace.
     * @param length
     *            length of the text to replace.
     * @param s
     *            the replacement.
     */
    void replace(int index, int length, String s);

    /**
     * Uses the underlying editors smart insert if available.
     * Inserts at the current cursor position.
     *
     * @param s the string to insert
     */
    void smartInsert(int index, String s);

    /**
     * Retrieves a substring from the text.
     *
     * @param index
     *            start of the substring.
     * @param length
     *            length of the substring.
     * @return the specified substring of the text.
     */
    String getText(int index, int length);

    /**
     * @return length of text
     */
	int getTextLength();

	Space getSpace();

    /**
     * Searches for the a keyword.
     *
     * @param search
     *            the parameters of the search.
     * @param offset
     *            where to start searching.
     * @return the index of the searched string.
     */
	SearchResult find(Search search, Position start);
}