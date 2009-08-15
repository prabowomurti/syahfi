
/**
 *
 * @author sangprabo
 *
 */

import javax.microedition.lcdui.*;
class Statistic extends Form implements CommandListener{
    private Syahfi midlet;
    private Display display;
    
    private Command cmBack;
    private Store store;
    
    public Statistic(Syahfi midlet, Display display){
        super("Statistic");
        this.midlet = midlet;
        this.display = display;
        
        this.cmBack = new Command("Back", Command.BACK, 1);
        addCommand(cmBack);
        
        this.store = new Store(Store.storageName);
        this.store.open();
        
        int[] cardData = store.getCardIds();
        int[] numOfCardWithGrade = new int[6];
        int numOfCards = cardData.length, nonMemorisedCards = 0;
        for (int i=0; i<numOfCards; i++){
            numOfCardWithGrade[this.store.getFieldOfCard(cardData[i], 1)]++;//check card's grade
            if (this.store.isDueForAcquisitionRep(cardData[i])){
                nonMemorisedCards++;
            }
        }
        
        append("Statistic for cards\nTotal cards : " + numOfCards + "\n");
        append("Grade 0 cards : " + numOfCardWithGrade[0] + "\n");
        append("Grade 1 cards : " + numOfCardWithGrade[1] + "\n");
        append("Grade 2 cards : " + numOfCardWithGrade[2] + "\n");
        append("Grade 3 cards : " + numOfCardWithGrade[3] + "\n");
        append("Grade 4 cards : " + numOfCardWithGrade[4] + "\n");
        append("Grade 5 cards : " + numOfCardWithGrade[5] + "\n");
        append("Non memorised cards : " + nonMemorisedCards + "\n__________\n");
        
        append("Scheduled cards for the next days\n");
        int oldCumulative = 0;
        int cumulative;
        for (int i=0;i<8;i++){
            cumulative = this.store.scheduledItems(i);
            append("In " + String.valueOf(i) + " day(s) : " +
                    String.valueOf(cumulative - oldCumulative) + "\n");
            oldCumulative = cumulative;
        }
        
        this.store.close();
        
        setCommandListener(this);
        
    }

    public void commandAction(Command c, Displayable s) {
        if (c == cmBack){
            Learn learn = new Learn(midlet, display);
            display.setCurrent(learn);
        }
    }
    
    
    
}
