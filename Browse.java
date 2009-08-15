
/**
 *
 * @author sangprabo
 *
 */

import java.util.*;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.lcdui.*;

public class Browse extends Form implements CommandListener{
    private String currDirName;
    private Syahfi midlet;
    private Display display;
    
    private Command cmSelect = new Command("Select", Command.ITEM, 1);
    private Command cmBack = new Command("Back", Command.BACK, 1);
    
    private final static String UP_DIRECTORY = "..";
    private final static String MEGA_ROOT = "/";
    private final static String SEP_STR = "/";
    private final static char SEP = '/';
    
    public Browse(Syahfi midlet, Display display){
        super("Browse File");
        this.midlet = midlet;
        this.display = display;
        
        
        currDirName = MEGA_ROOT;
        try {
            new Thread (new Runnable() {
                public void run() {
                    showCurrDir();
                }
            }).start();
        }catch (SecurityException se) {
            Alert alert = new Alert("Error", "You're not authorized", null, AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);

            Form form = new Form("Cannot access FileConnection");
            form.append(new StringItem(null, "You cannot run this MIDLET with the current permissions."));
            form.addCommand(cmBack);
            form.setCommandListener(this);
            display.setCurrent(alert, form);
        }catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public void commandAction(Command c, Displayable d){
        if (c == cmSelect){
            List curr = (List) d;
            final String currFile = curr.getString(curr.getSelectedIndex());
            new Thread (new Runnable() {
                public void run() {
                    if (currFile.endsWith(SEP_STR) || currFile.equals(UP_DIRECTORY)){
                        traverseDirectory(currFile);
                    }else {
                        //select the file and go to import page
                        display.setCurrent(new ImportCard(midlet, display, currDirName+currFile));
                    }
                }
            }).start();
            
        }else if (c == cmBack){
            display.setCurrent(new ImportCard(midlet, display, ""));
        }
    }
    
    public void showCurrDir() {
        Enumeration e;
        FileConnection currDir = null;
        List browser;
        try {
            if (MEGA_ROOT.equals(currDirName)) {
                e = FileSystemRegistry.listRoots();
                browser = new List(currDirName, List.IMPLICIT);
            }else {
                currDir = (FileConnection) Connector.open("file://localhost/" + currDirName);
                e = currDir.list();
                browser = new List(currDirName, List.IMPLICIT);
                //not root - draw UP_DIRECTORY
                browser.append(UP_DIRECTORY, null);
            }
            
            while (e.hasMoreElements()){
                String fileName = (String)e.nextElement();
                //since there is no icon for directory and file
                browser.append(fileName, null);
            }
            
            browser.setSelectCommand(cmSelect);
            browser.addCommand(cmBack);
            browser.setCommandListener(this);
            
            if (currDir != null) {
                currDir.close();
            }
            
            display.setCurrent(browser);
            
        } catch (IOException ioe) {
            ioe.printStackTrace();
            
        }
    }
    
    private void traverseDirectory(String fileName) {
        //In case of directory just change the current dir and show it
        if (currDirName.equals(MEGA_ROOT)) {
            if (fileName.equals(UP_DIRECTORY)) {
                //can not go up from MEGA_ROOT
                return;
            }
            currDirName = fileName;
        }else if (fileName.equals(UP_DIRECTORY)){
            //up one dir
            int i = currDirName.lastIndexOf(SEP, currDirName.length()-2);
            if (i != -1){//no search result
                currDirName = currDirName.substring(0, i+1);
            }else {
                currDirName = MEGA_ROOT;
            }
        }else {
            currDirName = currDirName + fileName;
        }
        
        showCurrDir();
    }
}
