package org.systemsbiology.genomebrowser.visualization;

import java.awt.Image;

/*
 * Interface to decouple visualization inteface from ui.
 */
public interface View {
    int getWidth();
    int getHeight();
    void updateImage(Image image);
    Image createImage(int width, int height);
}
