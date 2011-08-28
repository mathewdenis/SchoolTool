package org.bob.school.tools;

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
	 * @param delimiter String delimiting array elements
	 * @param prefix Constant String in front of an array element
	 * @param postfix Constant String behind an array element
	 * @return
	 */
	static public String arrayToString(Object[] array, String delimiter, String prefix, String postfix) {
		StringBuilder b = new StringBuilder();
		for(int i=0; i<array.length; ++i) {
			if(i!=0)
				b.append(delimiter);
			if(prefix != null)
				b.append(prefix);
			if(array[i] != null)
				b.append(array[i]);
			if(postfix != null)
				b.append(postfix);
		}
		return b.toString();
	}

	static public String arrayToString(Object[] array, String delimiter) {
		return arrayToString(array, delimiter, null, null);
	}

	static public String writeZeroIfNull(String s) {
		return s==null?"0":s;
	}
}
