package pl.polak.clicknumberpicker;

public final class NumberFormatUtils {

    public static String provideFloatFormater(int decimalLength) {
        StringBuilder builder = new StringBuilder();
        builder.append("%.");
        builder.append(String.valueOf(decimalLength));
        builder.append("f");
        return builder.toString();
    }

}
