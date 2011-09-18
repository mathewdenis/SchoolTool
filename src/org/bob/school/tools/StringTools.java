package org.bob.school.tools;

import java.util.Map;

public class StringTools {

	/** Join function similiar to perl's join.
	 * Return a string containing the String-representation of the values in the arrays,
	 * delimited by <code>delimiter</code>, pre- and postfixed by <code>prefix</code> and
	 * <code>postfix</code>.<br />
	 * <em>Example:</em> Consider:
	 * <pre>
	 *   String[] a =  { "two", "three", "four"};
	 *   String output = arrayToString(a, ". ", "Mike has ", "beers");
	 * </pre>
	 * 
	 * The output string <code>output</code> then contains (mind the spaces)
	 * <pre>
	 *   output = "Mike has two beers. Mike has three beers. Mike has four beers" 
	 * </pre>
	 * 
	 * @param array Array of objects to be joined
	 * @param translator Translation mapping taking each string from the array into a string .
	 *   If the string is not contained in the translator map the original string
	 *   is taken directly. If null no translation is applied.
	 * @param delimiter String delimiting array elements
	 * @param prefix Constant String in front of an array element
	 * @param postfix Constant String behind an array element
	 * @return The string delimited by delimeter, pre- and postfixed by the given strings
	 */
	static public String arrayToString(Object[] array, Map<String, String> translator, String delimiter, String prefix, String postfix) {
		StringBuilder b = new StringBuilder();
		for(int i=0; i<array.length; ++i) {
			if(i!=0)
				b.append(delimiter);
			if(prefix != null)
				b.append(prefix);
			if(array[i] != null)
				if(translator != null && translator.containsKey(array[i]))
					b.append(translator.get(array[i]));
				else
					b.append(array[i]);
			if(postfix != null)
				b.append(postfix);
		}
		return b.toString();
	}

	/** Join function similiar to perl's join.
	 * Return a string containing the String-representation of the values in the arrays,
	 * delimited by <code>delimiter</code>, pre- and postfixed by <code>prefix</code> and
	 * <code>postfix</code>.<br />
	 * <em>Example:</em> Consider:
	 * <pre>
	 *   String[] a =  { "two", "three", "four"};
	 *   String output = arrayToString(a, ". ", "Mike has ", "beers");
	 * </pre>
	 * 
	 * The output string <code>output</code> then contains (mind the spaces)
	 * <pre>
	 *   output = "Mike has two beers. Mike has three beers. Mike has four beers" 
	 * </pre>
	 * 
	 * @param array Array of objects to be joined
	 * @param delimiter String delimiting array elements
	 * @param prefix Constant String in front of an array element
	 * @param postfix Constant String behind an array element
	 * @return The string delimited by delimeter, pre- and postfixed by the given strings
	 */
	static public String arrayToString(Object[] array, String delimiter, String prefix, String postfix) {
		return arrayToString(array, null, delimiter, prefix, postfix); 
	}

	static public String arrayToString(Object[] array, String delimiter) {
		return arrayToString(array, delimiter, null, null);
	}

	static public String writeZeroIfNull(String s) {
		return s==null?"0":s;
	}
}
