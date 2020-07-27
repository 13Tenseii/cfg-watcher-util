package service.parser;

public class BracketSearcher {
    protected int findClosingBracketPos(char openBracket, char closeBracket, int starPos, String text) {
        int bracketIndex = 0;
        int idx = text.indexOf(openBracket, starPos);
        if(idx < 0)
            return -1;

        do{
            if (text.charAt(idx) == openBracket) {
                bracketIndex++;
            } else if (text.charAt(idx) == closeBracket) {
                bracketIndex--;
            }
            idx++;
        }while (bracketIndex != 0 && idx < text.length());
        if(bracketIndex != 0)
            return -1;
        return idx;
    }

    public int findCurvyClosingBracketPos(String text, int startIdx) {
        return findClosingBracketPos('{', '}', startIdx, text);
    }

    public int findRectClosingBracketPos(String text, int startIdx) {
        return findClosingBracketPos('[', ']', startIdx, text);
    }

    public String getContentInsideCurvyBrackets(String text, int openBracketPos){
        int to = findCurvyClosingBracketPos(text, openBracketPos);
        return text.substring(openBracketPos + 1, to);
    }
}