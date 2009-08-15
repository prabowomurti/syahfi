
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

/**
 * @author sangprabo
 */
public class Syahfi extends MIDlet {
    Display display;
    private Learn learn;
    
    public Syahfi() {
        display = Display.getDisplay(this);
    }
    public void startApp() {
        learn = new Learn(this, display);
        display.setCurrent(learn);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }
    
    public void exitMidlet(){
        destroyApp(false);
        notifyDestroyed();
    }
}
