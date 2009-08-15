/**
 *
 * @author sangprabo
 */

import javax.microedition.lcdui.*;
public class Card extends Form implements CommandListener, ItemCommandListener, ItemStateListener{
    private Syahfi midlet;
    private Display display;
    
    private Command cmBack, cmAddNew, cmDeleteSelected, cmSelectOtherwise,
            cmSearch, cmEdit;
    private TextField tfSearch;
    private StringItem siSearchButton, siEditButton;
    
    private String[][] cardData;
    
    private ChoiceGroup cgCards;
    
    public Card(Syahfi midlet, Display display){
        super("Cards");
        this.midlet = midlet;
        this.display = display;
        
        cmBack = new Command("Back", Command.BACK, 1);
        cmAddNew = new Command("Add new", Command.OK, 1);
        cmDeleteSelected = new Command("Delete Selected", Command.OK, 1);
        cmSelectOtherwise = new Command("Select Inverse", Command.OK, 1);
        
        addCommand(cmBack);
        addCommand(cmAddNew);
        addCommand(cmDeleteSelected);
        addCommand(cmSelectOtherwise);
        
        tfSearch = new TextField("", "", 20, TextField.ANY);
        append(tfSearch);
        
        //Search button
        siSearchButton = new StringItem(null, "Search", Item.BUTTON);
        cmSearch = new Command("Search", Command.OK, 1);
        siSearchButton.setDefaultCommand(cmSearch);
        siSearchButton.setItemCommandListener(this);
        append(siSearchButton);
        setItemStateListener(this);
        
        //Edit Card Button
        siEditButton = new StringItem(null, "Edit Card", Item.BUTTON);
        cmEdit = new Command("Edit", Command.OK, 1);
        siEditButton.setDefaultCommand(cmEdit);
        siEditButton.setItemCommandListener(this);
        append(siEditButton);
        setItemStateListener(this);
        
        //show all cards here... 
        cgCards = new ChoiceGroup("CARDS", Choice.MULTIPLE);
        Store store = new Store(Store.storageName);
        store.open();
        int numOfCards = store.numOfCards();
        cardData = store.readCardsWithIdQuestionAnswer();
        store.close();
        
        for (int i=0; i<numOfCards; i++){
            cgCards.append("ID:"+ cardData[i][0]+"\nQ: "+ cardData[i][1] + "\nA: "+ cardData[i][2], null);
        }
        append(cgCards);
        
        setCommandListener(this);
    }
    
    public void commandAction(Command c, Displayable s){
        if (c == cmBack){
            Learn learn = new Learn(midlet, display);
            display.setCurrent(learn);
        } else if (c == cmAddNew){
            Store store = new Store(Store.storageName);
            store.open();
            if (store.numOfCards() >= store.maximumNumberOfCardsAllowed){
                store.close();
                Alert al = new Alert("Alert", "Sorry, no space. Delete some old cards", null, AlertType.INFO);
                display.setCurrent(al);
            }else {
                store.close();
                EditCard ec = new EditCard(midlet, display, 0);
                display.setCurrent(ec);
            }
            
        } else if (c == cmDeleteSelected){
            Store store = new Store(Store.storageName);
            store.open();
            boolean selected[] = new boolean[cgCards.size()];
            cgCards.getSelectedFlags(selected);
            for (int i=0;i<selected.length;i++){
                if (selected[i]){
                    store.deleteCard(Integer.parseInt(cardData[i][0]));
                }
            }
            store.close();
            
            Card card = new Card(midlet, display);
            display.setCurrent(card);
        } else if (c == cmSelectOtherwise){
            for (int i=0;i<cgCards.size();i++){
                cgCards.setSelectedIndex(i, !cgCards.isSelected(i));
            }
        }
    }

    public void commandAction(Command c, Item item) {
        if (c == cmSearch){
            Store store = new Store(Store.storageName);
            store.open();
            cardData = null;
            cardData = store.searchCard(tfSearch.getString());
            cgCards.deleteAll();
            if (cardData != null) {
                for (int i=0; i<cardData.length; i++){
                    cgCards.append("ID:"+ cardData[i][0]+
                               "\nQ: "+ cardData[i][1] +
                               "\nA: "+ cardData[i][2], null);
                }
            }
            store.close();
            
        }else if ( c == cmEdit ){
            String cId = tfSearch.getString().trim();
            int cIdInt;
            try {
                cIdInt = Integer.parseInt(cId);
            }catch (NumberFormatException nfe){
                cIdInt = 0;
            }
            
            if ( cId.equals("") | cIdInt <=0){
                Alert al = new Alert("Error", "Not valid CardID", null, AlertType.ERROR);
                al.setTimeout(Alert.FOREVER);
                display.setCurrent(al);
            }else {
                Store store = new Store(Store.storageName);
                store.open();
                if ( store.isCardEmpty(cIdInt) ){
                    store.close();
                    Alert al = new Alert("Error", "ID doesn't exist", null, AlertType.INFO);
                    al.setTimeout(Alert.FOREVER);
                    display.setCurrent(al);
                }else {
                    store.close();
                    EditCard ec = new EditCard(midlet, display, cIdInt);
                    display.setCurrent(ec);
                }
            }
            
        }
    }
    
    public void itemStateChanged(Item item){
    }
}
