package steganograph;

import com.sun.javaws.exceptions.InvalidArgumentException;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Steganograph {
    // Class for encoding and decoding information inside images
    @Getter
    @Setter
    private int offset = 0;

    private String imagePath;
    private BufferedImage image;

    public void open(String imagePath) {
        /* Opens an image from given path */
        try {
            File file = new File(imagePath);
            image = ImageIO.read(file);

            this.imagePath = imagePath;
            offset = 0;
        } catch(IOException e) {
            System.out.format("Could not open file %s:\n", imagePath);
            e.printStackTrace();
        }
    }

    public void setImage(BufferedImage image) {
        // Sets image to the argument and resets offset
        this.image = image;
        offset = 0;
        imagePath = null;
    }

    public void setImageCopy(BufferedImage image) {
        // Sets the current image to a copy of this
        this.image = new BufferedImage(BufferedImage.TYPE_4BYTE_ABGR, image.getWidth(), image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                this.image.setRGB(x, y, image.getRGB(x, y));
            }
        }

        offset = 0;
        imagePath = null;
    }

    public void save(String path) {
        /* Saves image to given path - or automatically generates name if no given */
        if (image == null) {
            throw new IndexOutOfBoundsException("No file open");
        }

        if (path == null) {
            if (imagePath == null) {
                path = "result.png";
            } else {
                path = imagePath.split("\\.")[0] + "_result." + imagePath.split("\\.")[1];
            }
        }

        File file;
        try{
            file = new File(path);
            ImageIO.write(image, path.split("\\.")[1], file);
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    public void save() { save(null); }

    public int size() {
        // Returns size - how many bytes can the image store
        if (image == null) {
            return 0;
        }
        return image.getHeight() * image.getWidth() * 3 / 4;
    }

    public void put(byte value) {
        // Places a byte inside our image
        // Increments offset by one

        if (offset < size()) {
            // Because each pixel can store only 6 bits, we need
            // to calculate the realOffset in bytes and offset inside pixel
            int realOffset = (int) Math.floor((double) offset / 3 * 4);
            // pixelOffset - offset inside pixel (whether to store in r, g or b)
            int pixelOffset = offset % 3;

            int i = 0;
            // Put our 2-bit-parts 4 times
            while (i < 4) {
                // Get coordinates
                int x = realOffset % image.getWidth();
                int y = realOffset / image.getWidth();

                // Put message
                Pixel pixel = new Pixel(image.getRGB(x, y));
                pixel.addMessagePart(value, i, pixelOffset + 1);
                image.setRGB(x, y, pixel.getArgb());

                i++;
                pixelOffset += 1;
                if (pixelOffset == 3) {
                    realOffset += 1;
                    pixelOffset = 0;
                }
            }

            offset += 1;
        } else {
            throw new IndexOutOfBoundsException("No more pixels to save data to");
        }
    }

    public byte get() {
        // Gets a single byte of information from image - from current offset position
        // Increments offset

        if (offset < size()) {
            // Calculate the real offset (because 1 px can store only 6 bits)
            // and start offset inside pixel
            int realOffset = (int) Math.floor((double) offset / 3 * 4);
            int pixelOffset = offset % 3;

            // Collect all the message parts and save in result
            int result = 0;
            int i = 0;
            while (i < 4) {
                int x = realOffset % image.getWidth();
                int y = realOffset / image.getWidth();

                Pixel pixel = new Pixel(image.getRGB(x, y));
                result = result | pixel.getMessagePart(i, pixelOffset + 1);

                i++;
                pixelOffset += 1;
                if (pixelOffset == 3) {
                    realOffset += 1;
                    pixelOffset = 0;
                }
            }

            offset += 1;
            return (byte) result;
        } else {
            throw new IndexOutOfBoundsException("No more pixels to get data from");
        }

    }

    public void putInt(int value) {
        put((byte) (value>>>24 & 0xFF));
        put((byte) (value>>>16 & 0xFF));
        put((byte) (value>>>8 & 0xFF));
        put((byte) (value & 0xFF));
    }

    public int getInt() {
        return get()<<24 & 0xFF000000 | get()<<16 & 0xFF0000 | get()<<8 & 0xFF00 | get() & 0xFF;
    }

    public void putChar(char value) {
        put((byte) (value>>>8 & 0xFF));
        put((byte) (value & 0xFF));
    }

    public char getChar() {
        return (char) (get()<<8 & 0xFF00 | get() & 0xFF);
    }

    public void putString(String value) {
        putInt(value.length());

        for (int i = 0; i < value.length(); i++) {
            putChar(value.charAt(i));
        }
    }

    public String getString() {
        StringBuilder builder = new StringBuilder();

        int length = getInt();
        for (int i = 0; i < length; i++) {
            builder.append(getChar());
        }

        return builder.toString();
    }


    private static class Pixel {
        // Pixel class for handling of the manipulations inside each pixel
        private int argb;

        public Pixel(int argb) {
            this.argb = argb;
        }

        public void addMessagePart(int message, int part, int pixelPart) {
            // Places the part of the message in the appropriate pixel color
            // pixel part: 0-a 1-r 2-g 3-b
            int value = (message>>>(2 * (3 - part)) & 0b11);
            argb = (argb & (int) (0xFFFFFFFCFFFFFFL >>> (pixelPart * 8)));
            argb = argb | value<<(24 - pixelPart * 8);

        }

        public int getMessagePart(int part, int pixelPart) {
            // Retuns the message part
            return (argb>>>(24 - pixelPart * 8) & 0b11) << ((3 - part) * 2);
        }

        public int getArgb() {
            return argb;
        }

        public int[] getColors() {
            // Function returns colors as array - for debugging reasons
            int b = (argb & 0xFF);
            int g = (argb>>8 & 0xFF);
            int r = (argb>>16 & 0xFF);
            int a = (argb>>24 & 0xFF);

            return new int[] {a, r, g, b};
        }

        public String toString() {
            int[] colors = getColors();
            return String.format("argb(%d, %d, %d, %d)", colors[0], colors[1], colors[2], colors[3]);
        }
    }
}
