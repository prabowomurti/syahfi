
/**
 *
 * @author sangprabo
 */
import javax.microedition.lcdui.*;
class EditCard extends Form implements CommandListener{
    private Syahfi midlet;
    private Display display;
    
    private Command cmBack, cmSave;
    private TextField tfQuestion, tfAnswer;
    private ChoiceGroup cgGrade;
    
    private String[] cardData;
    private String cardId;
    
    public EditCard (Syahfi midlet, Display display, int cardId){
        super("Add/Edit Card");
        this.midlet = midlet;
        this.display = display;
        this.cardId = String.valueOf(cardId);
        
        cmBack = new Command("Back", Command.BACK, 1);
        cmSave = new Command("Save", Command.OK, 1);
        addCommand(cmBack);
        addCommand(cmSave);
        
        tfQuestion = new TextField("Question", "", 50, TextField.ANY);
        tfAnswer = new TextField("Answer", "", 50, TextField.ANY);
        
        append(tfQuestion);
        append(tfAnswer);
        
        
        //UNTESTED
        //it comes from class Card, and if cardId is not 0, it means it will be "Edit Card Menu"
        if (cardId > 0){
            Store store = new Store(Store.storageName);
            store.open();
            this.cardData = new String[store.numOfCardAttributes+1];
            this.cardData = store.readCardWithID(cardId);
            tfQuestion.setString(this.cardData[10]);
            tfAnswer.setString(this.cardData[11]);
            store.close();
        }else {//if cardId == 0 it means addCard menu
            cgGrade = new ChoiceGroup("Grade", ChoiceGroup.EXCLUSIVE);
            cgGrade.append("0", null);
            cgGrade.append("1", null);
            cgGrade.append("2", null);
            cgGrade.append("3", null);
            cgGrade.append("4", null);
            cgGrade.append("5", null);
            append(cgGrade);
        }
        //UNTIL HERE
        
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable s) {
        if (c == cmBack){
            Card card = new Card(midlet, display);
            display.setCurrent(card);
        } else if (c == cmSave){
            if (tfQuestion.getString().equals("") || tfAnswer.getString().equals("")){
                Alert al = new Alert("Alert", "Question or Answer can't be empty", null, AlertType.INFO);
                al.setTimeout(Alert.FOREVER);
                display.setCurrent(al);
            }else {//if Q and A exist
                Store store = new Store(Store.storageName);
                store.open();
                //there is no need to use isCardEmpty(), already checked at Card Class
                //if there is an ID, run EditCard()
                if ( !this.cardId.equals("0") ){
                    if (store.editCard(Integer.parseInt(cardData[0]), tfQuestion.getString(), tfAnswer.getString())){
                        Alert al = new Alert("Card added", "Card's succesfully saved", null, AlertType.INFO);
                        al.setTimeout(Alert.FOREVER);
                        display.setCurrent(al);

                    }else {
                        Alert al = new Alert("Error", "Something wrong happened", null, AlertType.INFO);
                        al.setTimeout(Alert.FOREVER);
                        display.setCurrent(al);
                    }
                }else {//if there is no ID, it means addNewCard
                    if (store.addNewCard(cgGrade.getSelectedIndex(), tfQuestion.getString(), tfAnswer.getString())){
                        Alert al = new Alert("Card added", "Card's succesfully saved", null, AlertType.INFO);
                        al.setTimeout(Alert.FOREVER);
                        display.setCurrent(al);

                        //make the fields blank, and 
                        tfQuestion.setString("");
                        tfAnswer.setString("");
                        cgGrade.setSelectedIndex(0, true);
                    }else {
                        Alert al = new Alert("Error", "Something wrong happened", null, AlertType.INFO);
                        al.setTimeout(Alert.FOREVER);
                        display.setCurrent(al);
                    }
                }
                    
                store.close();//close everything that you've opened
            }
        }
    }
    
}
