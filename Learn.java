
/**
 *
 * @author sangprabo
 */

import javax.microedition.lcdui.*;
import java.util.Vector;
class Learn extends Form implements CommandListener, ItemCommandListener{

    private Display display;
    private Syahfi midlet;
    private Command cmExit, cmSetting, cmStatistics, cmCards, cmHelp, cmAbout,
            cmBtShowAnswer, cmImport, cmBtGrade, cmBtLearnAhead;
    private StringItem siQuestion, siAnswer, btShowAnswer, btGrade, btLearnAhead;
    private ChoiceGroup cgGrade;
    
    private static Vector revisionQueue = new Vector();
    private Store store = new Store(Store.storageName);
    private int cardId;
    String cardQuestion = "", cardAnswer = "";

    public Learn (Syahfi midlet, Display display){
        super ("Learn");
        this.display = display;
        this.midlet  = midlet;
        
        cmExit = new Command("Exit", Command.EXIT, 1);
        cmImport = new Command("Import", Command.ITEM, 1);
        cmSetting = new Command("Settings", Command.ITEM, 1);
        cmStatistics = new Command("Statistics", Command.ITEM, 1);
        cmCards = new Command("Edit Cards", Command.ITEM, 1);
        cmHelp = new Command("Help", Command.ITEM, 1);
        cmAbout = new Command("About", Command.ITEM, 1);

        
        addCommand(cmCards);
        addCommand(cmExit);
        addCommand(cmImport);
        addCommand(cmSetting);
        addCommand(cmStatistics);
        addCommand(cmHelp);
        addCommand(cmAbout);
        setCommandListener(this);
        
        siQuestion = new StringItem("Question\n", "");
        siQuestion.setItemCommandListener(this);
        
        siAnswer = new StringItem("Answer\n", "");
        siAnswer.setItemCommandListener(this);
        
        
        btShowAnswer = new StringItem("", "Show Answer",Item.BUTTON);
        cmBtShowAnswer = new Command("ShowAnswer", Command.OK, 1);
        btShowAnswer.setDefaultCommand(cmBtShowAnswer);
        btShowAnswer.setItemCommandListener(this);

        cgGrade = new ChoiceGroup("Grade", ChoiceGroup.EXCLUSIVE);
        for (int i=0; i<6; i++){
            cgGrade.append(String.valueOf(i), null);
        }
        
        
        btGrade = new StringItem("", "Next", Item.BUTTON);
        cmBtGrade = new Command("Grade", Command.OK, 1);
        btGrade.setDefaultCommand(cmBtGrade);
        btGrade.setItemCommandListener(this);
        
        
        btLearnAhead = new StringItem("", "Learn Ahead?", Item.BUTTON);
        cmBtLearnAhead = new Command("LearnAhead", Command.OK, 1);
        btLearnAhead.setDefaultCommand(cmBtLearnAhead);
        btLearnAhead.setItemCommandListener(this);
        
        
//      store.delete();
//
//        /*
        store.open();
        int numOfCards = store.numOfCards();
        store.close();
        if (numOfCards == 0){
            append("Welcome to Syahfi! \n" +
                    "To start learning, please add some cards from 'Edit Cards'"+
                    "-> 'Add Card' menu. \n\n" +
                    "Happy learning! :)");
        }else {
            showQuestion();
        }
//*/
    }
    
    public void commandAction(Command c, Displayable s) {
        if (c == cmExit){
            midlet.exitMidlet();
        }else if (c == cmImport) {
            //we leave 3rd parameter blank, it's filePath
            display.setCurrent(new ImportCard(midlet, display, ""));
        }else if (c == cmSetting){
            Setting setting = new Setting(midlet, display);
            display.setCurrent(setting);
        }else if (c == cmStatistics){
            Statistic statistic = new Statistic(midlet, display);
            display.setCurrent(statistic);
        }else if (c == cmCards){
            Card card = new Card(midlet, display);
            display.setCurrent(card);
        }else if (c == cmAbout){
            Alert al = new Alert("About", "Syahfi v2.0.0\n" +
                    "Main author: Prabowo Murti under GNU Public License\n" + 
                    "More? www.prabowomurti.com\n", null, AlertType.INFO);
            al.setTimeout(Alert.FOREVER);
            display.setCurrent(al);
        }else if (c == cmHelp){
            Alert al = new Alert("Help", "\nSyahfi is a flash"+
                    "card application. It helps you to memorize something. " +
                    "This is how it works:\n" +
                    "0. You add several cards on 'Edit Cards'-> 'Add Card'\n" +
                    "1. A card is simply a pair of question and answer (that you " +
                    "want to memorize). Set it's initial grade. A grade of card" +
                    " is a rate, how well you remember it. Grade 0 means you " +
                    "can not remember it well, and grade 5 means you could " +
                    "remember it without problem\n" +
                    "2. Add some cards\n" +
                    "3. Back to main Menu and start Learn. Syahfi will keep on " +
                    "asking you questions until you give a grade 2 (or higher) " +
                    "to the card\n" +
                    "4. Syahfi will schedule the cards and calculate the best " +
                    "interval time for you to learn it again. It's the best way " +
                    "to learn\n" +
                    "5. Learn your cards everyday to get the optimum result\n" +
                    "6. You could see some statistic from main Menu -> 'Statistic'\n" +
                    "7. To edit cards, go to main Menu -> 'Edit Cards' and choose " +
                    "a card you want to edit by typing its ID on the available " +
                    "input text and press Edit button\n"+
                    "8. To get more help, don't be shame to send me an email at " +
                    "prabowo.murti@gmail.com", null, AlertType.INFO);
            al.setTimeout(Alert.FOREVER);
            display.setCurrent(al);
        }
    }
    
    //still considering how these two could be better...
    public static void setRevQueue(Vector vector){
        Learn.revisionQueue = vector;
    }
    
    public static Vector getRevQueue(){
        return Learn.revisionQueue;
    }

    public void commandAction(Command co, Item item) {
        /*when user press 'show answer' button*/
        if (co == cmBtShowAnswer){
           delete(1);//delete btShowAnswer
           append(siAnswer);
           siAnswer.setText(cardAnswer);
           append(cgGrade);
           append(btGrade);
        }else if (co == cmBtGrade){
            processAnswer();
        }else if (co == cmBtLearnAhead){
            store.open();
            revisionQueue = store.rebuildRevisionQueue(true);
            store.close();
            showQuestion();
        }
    }

    private void showQuestion(){
        store.open();
        if ( revisionQueue.size() == 0 ){
            revisionQueue = store.rebuildRevisionQueue(false);
        }

        deleteAll();
        if ( revisionQueue.size() != 0){
            
            //get first question and remove it from revisionQueue AFTER user grade it
            cardId = Integer.parseInt((String) revisionQueue.firstElement());
            cardQuestion = store.getQuestionOfCard(cardId);
            cardAnswer = store.getAnswerOfCard(cardId);
            //cardGrade = store.getFieldOfCard(cardId, 1);

            store.close();
            //set text on siQuestion
            append(siQuestion);
            siQuestion.setText(cardQuestion);
            append(btShowAnswer);

        }else {
            //remove siQuestion
            deleteAll();
            append("There is no more scheduled card");
            //add button 'learn ahead'
            append(btLearnAhead);
            store.close();
        }
    }
    
    private void processAnswer(){
        store.open();
        store.processAnswer(cardId, cgGrade.getSelectedIndex());
        if (!revisionQueue.isEmpty()){
            revisionQueue.removeElementAt(0);
        }else {
            revisionQueue = store.rebuildRevisionQueue(false);
        }
        store.close();
        
        showQuestion();
    }
    
    
}
