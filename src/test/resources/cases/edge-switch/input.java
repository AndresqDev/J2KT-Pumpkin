package edge;

public class EdgeSwitch {
    public String name(int value) {
        String result = "";
        switch (value) {
            case 1:
                result = "one";
                break;
            case 2:
                result = "two";
            case 3:
                result = "three";
                break;
            default:
                result = "many";
        }
        return result;
    }

    public int expression(int value) {
        return switch (value) {
            case 1, 2 -> 10;
            case 3 -> 30;
            default -> 0;
        };
    }
}
