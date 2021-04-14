package texel.texel_pocketmaps.geocoding;

public class CityMatcher {
    String[] lines;
    boolean[] isNumeric;

    public CityMatcher(String searchS, boolean explicitSearch) {
        if (explicitSearch) {
            lines = new String[1];
            lines[0] = searchS;
        } else {
            lines = searchS.replace('\n', ' ').split(" ");
        }
        isNumeric = new boolean[lines.length];
        for (int i = 0; i < lines.length; i++) {
            isNumeric[i] = isNumeric(lines[i]);
            if (!isNumeric[i]) {
                lines[i] = lines[i].toLowerCase();
            }
        }
    }

    /**
     * Ignores ',' and '.' on check.
     **/
    public static boolean isNumeric(String s) {
        try {
            Integer.parseInt(s.replace('.', '0').replace(',', '0'));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isMatching(String value, boolean valueNumeric) {
//    char [] alphabet = {'ա', 'բ', 'գ', 'դ', 'ե' ,'զ', 'է', 'ը', 'թ', 'ժ', 'ի', 'լ', 'խ', 'ծ', 'կ', 'հ', 'ձ', 'ղ', 'ճ', 'մ',
//            'յ', 'ն', 'շ', 'ո', 'չ', 'պ', 'ջ', 'ռ', 'ս', 'վ', 'տ', 'ր', 'ց', 'ւ', 'փ', 'ք', 'օ', 'ֆ', 'և'};
//
//    for (char ch : alphabet)
//      if(value.contains(ch + ""))
//        return false;

        if (value == null) return false;
        if (value.isEmpty()) return false;
        if (!valueNumeric) value = value.toLowerCase();

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            if (valueNumeric && isNumeric[i] && value.equals(lines[i])) return true;
            if (!valueNumeric && !isNumeric[i]) {
                if (lines[i].length() < 3 && value.equals(lines[i])) return true;
                if (value.contains(lines[i])) return true;
            }
        }
        return false;
    }
}
