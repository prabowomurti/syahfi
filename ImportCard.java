
/**
 *
 * @author sangprabo
 */

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.lcdui.*;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class ImportCard extends Form implements CommandListener, ItemCommandListener{
    private Display display;
    private Syahfi midlet;
    private String fileName;
    
    private KXmlParser parser;
    
    private Command cmBack = new Command("Back", Command.BACK, 1);
    private Command cmImport = new Command("Import", Command.OK, 1);

    private TextField tfFileName = new TextField("XML File Name", null, 256, TextField.ANY);
    private StringItem btBrowse = new StringItem("", "Browse", Item.BUTTON);
    private Command cmBtBrowser = new Command("Browse", Command.ITEM, 1);

    //information from the store we need
    private Store store = new Store(Store.storageName);
    private int maximumCardsToAdd;
    
    public ImportCard(Syahfi midlet, Display display, String fileName){
        super("Import");
        this.display = display;
        this.midlet = midlet;
        this.fileName = fileName;
        
        store.open();
        this.maximumCardsToAdd = store.maximumNumberOfCardsAllowed - store.numOfCards();
        store.close();
        
        append(tfFileName);
        //dev phase
        //tfFileName.setString("/MemoryStick/default.xml");
        
        tfFileName.setString(fileName);
        
        append(btBrowse);
        addCommand(cmBack);
        addCommand(cmImport);
        setCommandListener(this);
        btBrowse.setDefaultCommand(cmBtBrowser);
        btBrowse.setItemCommandListener(this);
        
    }

    public void commandAction(Command c, Displayable s) {
        if (c == cmBack){
            display.setCurrent(new Learn(midlet, display));
        }else if (c == cmImport){
            //checking file type
            if (tfFileName.getString().endsWith(".xml")){
                new Thread (new Runnable() {
                public void run() {
                    doParseXML(getInputStream());
                }
                }).start();
            }else {
                Alert al = new Alert("Error", "Oops, looks like your file is" +
                        " not in XML format (*.xml)", null, AlertType.ERROR );
                al.setTimeout(Alert.FOREVER);
                display.setCurrent(al);
            }
            
        }
    }

    public void commandAction(Command co, Item item) {
        if (co == cmBtBrowser){
           display.setCurrent(new Browse(midlet, display));
        }
    }
    
    public InputStream getInputStream () {
        InputStream is = null;
            try {
                FileConnection fc = (FileConnection) Connector.open("file://"+tfFileName.getString());
                if (fc.exists()){
                    is = fc.openInputStream();
                }else {
                    //file doesn't exist
                    is = null;
                    display.setCurrent(new Alert("Error", "File doesn't exist", null, AlertType.ERROR));
                }
                fc.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
        return is;
    }
    
    public void doParseXML (InputStream inputStream){
        String[][] item = new String[maximumCardsToAdd][2];
        int itemCounter = 0;
        final String TAG_MNEMOSYNE = "mnemosyne",
                TAG_CATEGORY = "category",
                TAG_ITEM = "item",
                TAG_Q = "Q",
                TAG_A = "A",
                TAG_CAT = "cat";
        
        parser = new KXmlParser();
        try {
            parser.setInput(new InputStreamReader(inputStream));
            boolean insideCategoryTag = true;
            parser.next();
            parser.require(XmlPullParser.START_TAG, null, TAG_MNEMOSYNE);
            while (parser.nextTag() != XmlPullParser.END_TAG){
                while (insideCategoryTag){
                    parser.require(XmlPullParser.START_TAG, null, TAG_CATEGORY);
                    parser.skipSubTree();
                    parser.require(XmlPullParser.END_TAG, null, TAG_CATEGORY);
                    parser.nextTag();
                    
                    if (!parser.getName().equals(TAG_CATEGORY)){
                        insideCategoryTag = false;//out
                    }
                }

                parser.require(XmlPullParser.START_TAG, null, TAG_ITEM);
                while (parser.nextTag() != XmlPullParser.END_TAG){
                    //skip CAT
                    parser.require(XmlPullParser.START_TAG, null, TAG_CAT);
                    parser.skipSubTree();
                    parser.require(XmlPullParser.END_TAG, null, TAG_CAT);

                    parser.nextTag();
                    //question
                    parser.require(XmlPullParser.START_TAG, null, TAG_Q);
                        item[itemCounter][0] = (String)parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, TAG_Q);

                    parser.nextTag();
                    //answer
                    parser.require(XmlPullParser.START_TAG, null, TAG_A);
                        item[itemCounter][1] = (String)parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, TAG_A);
                    itemCounter++;
                    if (itemCounter >= maximumCardsToAdd){//stop if max value reached
                        importCard(item, itemCounter);
                        return;
                    }
                }
                parser.require(XmlPullParser.END_TAG, null, TAG_ITEM);
            }
            parser.require(XmlPullParser.END_TAG, null, TAG_MNEMOSYNE);

            importCard(item, itemCounter);

        } catch (XmlPullParserException xppe){
            Alert al = new Alert("Error", "Oops, looks like Syahfi can not " +
                    "import your XML file. Get some help or report problem " +
                    "to http://groups.google.com/group/syahfi-user", null, AlertType.ERROR);
            al.setTimeout(Alert.FOREVER);
            display.setCurrent(al);
        } catch (Exception e){
          e.printStackTrace();
        } finally {
            // try to close, and ignore any exceptions
            try { 
                parser = null;
            } catch (Exception ignored) {}
        }
        
    }

    /**
     * 
     * @param item String that holds all item's question and answer
     * @param itemCounter integer
     */
    public void importCard(String[][] item, int itemCounter){
        store.open();
        byte grade = 0;//assumption: from mnemosyne_core.py, the other values come from addNewCard()
        double easiness = store.averageEasiness();
        int ac_rp = 1;
        int rt_rp = 0;
        int lps = 0;
        int ac_rp_l = 1;
        int rt_rp_l = 0;
        int l_rp = store.daysSinceStart();
        int n_rp = l_rp;//actually it's l_rp + new_interval. Since the grade is 0, so new interval =0
        
        for (int i=0; i < itemCounter; i++){
            store.saveCard(store.getEmptySlots(), grade, easiness, ac_rp, rt_rp, lps, ac_rp_l, rt_rp_l, l_rp, n_rp,
                    item[i][0],//question
                    item[i][1] //answer
                    );
        }
        //increase numOfCards;
        int oldNumOfCards = store.numOfCards();
        store.saveRecord(4, String.valueOf(oldNumOfCards+itemCounter));
        store.close();

        Alert al = new Alert("Congrats", String.valueOf(itemCounter) + " card(s) already imported!", null, AlertType.INFO);
        al.setTimeout(Alert.FOREVER);
        display.setCurrent(al);
    }
}
