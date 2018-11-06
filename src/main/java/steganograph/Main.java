package steganograph;

public class Main {
    public static void main(String[] args) {
        StringBuilder textToSave = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            textToSave.append("Hello world! ");
        }

        Steganograph buffer = new Steganograph();
        buffer.open("mario.png");
        buffer.putString(textToSave.toString());
        buffer.save();

        Steganograph buffer2 = new Steganograph();
        buffer2.open("mario_result.png");
        System.out.println(buffer2.getString());
        System.out.println("Bytes in buffer filled: " + buffer2.getOffset());
        System.out.println("Total size: " + buffer2.size());
    }
}
