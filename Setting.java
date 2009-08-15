
/**
 *
 * @author sangprabo
 */

import javax.microedition.lcdui.*;
class Setting extends Form implements CommandListener {
    
    private Display display;
    private Syahfi midlet;
    
    private Store store;
    
    private Command cmBack, cmSave, cmReset;
    private TextField tfGrade0Cards2LearnAtOnce;
    private ChoiceGroup cgIsRandomOrderLearning;
    
    
    public Setting (Syahfi midlet, Display display){
        super("Preference");
        this.display = display;
        this.midlet = midlet;
        
        cmBack = new Command("Back",null, Command.CANCEL, 1);
        cmSave = new Command("Save", null, Command.OK, 2);
        cmReset = new Command("Set Default", null, Command.OK, 3);
        addCommand(cmBack);
        addCommand(cmSave);
        addCommand(cmReset);
        setCommandListener(this);
        
        this.tfGrade0Cards2LearnAtOnce = new TextField("Grade 0 cards to learn at once", "5" , 2, TextField.NUMERIC);
        append(tfGrade0Cards2LearnAtOnce);
        
        this.cgIsRandomOrderLearning = new ChoiceGroup("", ChoiceGroup.MULTIPLE);
        cgIsRandomOrderLearning.append("Learn new cards in random order", null);
        append(cgIsRandomOrderLearning);
        
        //about store? goes here
        store = new Store(Store.storageName);
        if (store.open()){
            tfGrade0Cards2LearnAtOnce.setString(store.readRecord(2));
            if (store.readRecord(3).equals("1")){
                cgIsRandomOrderLearning.setSelectedIndex(0, true);
            }
        }
        setCommandListener(this);
        
    }
    
    public void commandAction (Command c, Displayable s){
        if (c == cmBack){
            Learn learn = new Learn(midlet, display);
            display.setCurrent(learn);
        }else if (c == cmSave){
            if (tfGrade0Cards2LearnAtOnce.getString().equals("")){
                Alert al = new Alert("Error","All fields are required", null, AlertType.INFO);
                display.setCurrent(al);
            }else {
                if (store.open()){
                    store.saveRecord(2, tfGrade0Cards2LearnAtOnce.getString());
                    if (cgIsRandomOrderLearning.isSelected(0)){
                        store.saveRecord(3, "1");
                    }else {
                        store.saveRecord(3, "0");
                    }
                }
            }
        }else if (c == cmReset){
            
        }
    }
}
