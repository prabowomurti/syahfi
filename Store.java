
/**
 *
 * @author sangprabo
 */

import javax.microedition.rms.*;
import java.util.Random;
import java.util.Vector;

class Store {
    private RecordStore store;
    public static final String storageName = "SyahfiData";
    public static final int amount = 100;//allocation for each record, in byte
    private final int cardStartId = 11;//10 rows for setting and anothers
    public final int numOfCardAttributes = 11;
    public final int maximumNumberOfCardsAllowed = 300;//afraid of memory runout
    
    Store (String storageName) {
        store = null;
    }
    
    RecordStore getStore() {
        return store;
    }
    
    int getNumOfRecords() {
        try {
            return store.getNumRecords();
        } catch (RecordStoreException rse){
            return -1;// it means something wrong with the Recordstore
        }
    }
    
    int getNextRecordID() {
        try {
            return this.getStore().getNextRecordID();
        } catch (RecordStoreException rse){
            return 1;
        }
        
    }
    
    boolean open(){
        try {
            store = RecordStore.openRecordStore(Store.storageName, true);
        } catch (RecordStoreException rse) {
        }
        if (store == null)
            return false;
        try {
            if (this.getStore().getNumRecords() <=0)//if there is no store before, create and insert default data
                this.initiateData();
        } catch (RecordStoreNotOpenException rse){
            
        }
        return true;
    }
    
    void initiateData(){//do this each time a record store created or reset
        String timeOfStart = String.valueOf(System.currentTimeMillis()/1000);
        this.saveRecord(0, timeOfStart);//id 1
        this.saveRecord(0, "5");//id 2 : Number of 0 cards to learn at once
        this.saveRecord(0, "1");//id 3: learn new cards in random order, 0 means No, and 1 means Yes
        this.saveRecord(0, "0");//id 4: the length of Cards (banyaknya cards) used by numOfCard()
        this.saveRecord(0, ":");//id 5: define the slots where there is no card, or empty card (because of deletion)
        for (int i=6;i<cardStartId;i++)
            this.saveRecord(0,"");//reserved space for future development
    }
    
    boolean close() {
        try {
            if (store != null){
                store.closeRecordStore();
            }
        } catch (RecordStoreException rse){
            return false;
        }
        return true;
    }
    
    //wondering it's never used
    void delete() {
        try {
            RecordStore.deleteRecordStore(storageName);
        } catch (RecordStoreNotFoundException rse) {
        } catch (RecordStoreException rse){
        }
    }
    
    
    /** Save a content into recordstore
     * 
     * @param recID record ID
     * @param str new content
     */
    void saveRecord(int recID, String str) {
        byte[] rec = str.getBytes();
        try {
            if (recID == 0){
                store.addRecord(rec, 0, rec.length);
            } else {
                store.setRecord(recID, rec, 0, rec.length);
            }
        } catch (RecordStoreException rse){
            
        }
    }
    
    String readRecord(int recID) {
        try {
            byte[] recData = new byte[amount];
            int len;
            
            len = store.getRecord(recID, recData, 0);
            return new String(recData, 0, len);
        } catch (RecordStoreException rse){
            return null;
        } catch (NullPointerException rse){
            return null;
        }
    }
    
    boolean delRecord(int recID) {
        try {
            store.deleteRecord(recID);
            return true;
        } catch (RecordStoreException rse){
            return false;
        }
    }
    
    /** Make a record available for anything else
     * 
     * @param int recID
     * @return boolean
     */
    boolean blankRecord(int recID){
        if (recID >0){
            if (this.readRecord(recID).equals(null)){
                return false;
            }
            this.saveRecord(recID, "");
            return true;
        }else {
            return false;
        }
    }
    
    /** Get an empty slot that used for addition (for optimation)
     * empty slot could be a slot that available for addition because of deletion
     * 
     * @return first blank cardId in integer
     */
    public int getEmptySlots(){
        //check record, if contains only ":", there is no empty slot
        String emptySlots = readRecord(5);
        String blankCardId = "";
        if ( emptySlots.length() < 1 ){
            saveRecord(5, ":");
            return 0;
        }else if (emptySlots.length() == 1){
            return 0;
        }else {
            for (int i=1; i<emptySlots.length(); i++){
                if ( emptySlots.charAt(i+1) == ':' ){
                    blankCardId = emptySlots.substring(1,i+1);
                    emptySlots = ":" + emptySlots.substring(i+2, emptySlots.length());
                    break;
                }
            }
            saveRecord(5, emptySlots);//save the rest back
            return Integer.parseInt(blankCardId);
        }
    }
    
    public void addToEmptySlots(int cardId){
        saveRecord(5,readRecord(5)+String.valueOf(cardId)+":");
    }
    
//Since we don't use any search and replace string, we don't use this method
    /** Thanks to http://www.itgalary.com/forum_posts.asp?TID=871
     * 
     * @param _text Your text
     * @param _searchStr
     * @param _replacementStr
     * @return
     */
/* 
    public static String replace(String _text, String _searchStr, String _replacementStr){
   // String buffer to store str
   StringBuffer sb = new StringBuffer();

   // Search for search
   int searchStringPos = _text.indexOf(_searchStr);
   int startPos = 0;
   int searchStringLength = _searchStr.length();

   // Iterate to add string
   while (searchStringPos != -1) {
       sb.append(_text.substring(startPos, searchStringPos)).append(_replacementStr);
       startPos = searchStringPos + searchStringLength;
       searchStringPos = _text.indexOf(_searchStr, startPos);
   }

   // Create string
   sb.append(_text.substring(startPos,_text.length()));

   return sb.toString();
}
*/
    //it is when the card comes from the user, modified from add_new_item in Mnemosyne's method
    public boolean addNewCard(int grade, String question, String answer){
        if (numOfCards() >= maximumNumberOfCardsAllowed){//overload?
            return false;
        }else {
            //defining some default values
            int cardId = getEmptySlots();//check emptySlots() first, return 0 means new ID
            byte gr = (byte) grade;
            double easiness = averageEasiness();
            int l_rp = daysSinceStart();
            int newInterval = calculateInitialInterval(gr);
            newInterval += calculateIntervalNoise(newInterval);
            int n_rp = l_rp + newInterval;//I don't use daysSinceStart() more, I've already call it once!
            this.saveCard(
                    cardId,//if it's 0, it means it'll be located in new recordID
                    gr,//it is in 'byte' since grade ranges only from 0 to 5
                    easiness,
                    1,//ac_rp, see add_new_item about this default value
                    0,
                    0,
                    1,//ac_rp_l, see add_new_item about this default value
                    0,
                    l_rp,
                    n_rp,
                    question,
                    answer);
            
            //after a new card added, don't forget to ++ numOfCards()
            int oldNumOfCards = this.numOfCards();
            this.saveRecord(4, String.valueOf(oldNumOfCards+1));
            return true;
        }
    }
    
    //used for edit Card from menu Edit Card
    public boolean editCard(int cardId, String question, String answer){
        saveRecord(convertToRecordId(cardId)+9, question);//index of field question is located in the 10th
        saveRecord(convertToRecordId(cardId)+10, answer);//11th
        return true;
    }
    
    //saveCard is different with addNewCard(), it lists all of the attributes
    public void saveCard (
            int cardId,
            byte grade,
            double easiness,
            int ac_rp,
            int rt_rp,
            int lps,
            int ac_rp_l,
            int rt_rp_l,
            int l_rp,
            int n_rp,
            String question,
            String answer){
        String[] cardData = new String[(numOfCardAttributes+1)];
        cardData[0] = String.valueOf(cardId);
        cardData[1] = String.valueOf(grade);
        cardData[2] = String.valueOf(easiness);
        cardData[3] = String.valueOf(ac_rp);
        cardData[4] = String.valueOf(rt_rp);
        cardData[5] = String.valueOf(lps);
        cardData[6] = String.valueOf(ac_rp_l);
        cardData[7] = String.valueOf(rt_rp_l);
        cardData[8] = String.valueOf(l_rp);
        cardData[9] = String.valueOf(n_rp);
        cardData[10] = question;
        cardData[11] = answer;
        
        if (cardData[0].equals("0")){//add new cards
            for (int i=1;i<12;i++){
                saveRecord(0,cardData[i]);
            }
        }else {//edit saved cards
            //fixed bug
            int startRecordId = convertToRecordId(cardId);
            for (int i=1;i<=numOfCardAttributes;i++){
                saveRecord(i+startRecordId-1, cardData[i]);
            }
        }
    }
    /** This method is optimized-version of readAllCards (since we only need Q and
     * A section. Returning only ID, Q, and A.
     * 
     * @return cardData[][3] (0 ID, 1 Q, 2 A)
     */
    public String[][] readCardsWithIdQuestionAnswer(){
        String[][] cardData = new String[numOfCards()][3];
        int numOfCards = numOfCards();
        int i=0;//index
        int cardID = 1;
        
        while (i < numOfCards){
            if (!isCardEmpty(cardID)){
                cardData[i][0] = String.valueOf(cardID);
                cardData[i][1] = getQuestionOfCard(cardID);
                cardData[i][2] = getAnswerOfCard(cardID);
                i++;
            }
            cardID++;
        }
        return cardData;
    }
    
    //read card with ID id, return an array with length = numOfCardAttributes
    public String[] readCard (int id){
        if (id <= 0) {//validate id
            return null;
        }
        
        String[] cardData = new String[numOfCardAttributes];
        
        int j=0;
        int startRecordId = this.convertToRecordId(id);
        int endRecordId = startRecordId + numOfCardAttributes;
        for (int i=startRecordId;i<endRecordId;i++){
            cardData[j] = readRecord(i);
            j++;
        }
        return cardData;
    }
    
    //UNTESTED
    //search cards based on a String (search only on Question or Answer sections)
    public String[][] searchCard(String query){
        if (query.trim().equals("")){
            return readCardsWithIdQuestionAnswer();
        }
        
        int numOfCards = this.numOfCards();
        String[][] cardData = new String[numOfCards][3];
        int i=0;//index
        int cardID = 1;
        int j=0;//index of return cardData, and also means the number of results
                
        while (i < numOfCards){
            if (!isCardEmpty(cardID)){
                //check only in QUestion and Answer sections
                if (getQuestionOfCard(cardID).indexOf(query) != -1 | getAnswerOfCard(cardID).indexOf(query) != -1){
                    cardData[j][0] = String.valueOf(cardID);
                    cardData[j][1] = getQuestionOfCard(cardID);
                    cardData[j][2] = getAnswerOfCard(cardID);
                    j++;
                }
                i++;
            }
            cardID++;
        }
        
        if (j ==0 )
            return null;//zero result
        
        String[][] filteredCardData = new String[j][3];//'3' because it has ID, Q, and A
        for (int k=0;k<j;k++){
            filteredCardData[k] = cardData[k];
        }
        return filteredCardData;
    }
    
    //read card with ID = id, return an array with length = numOfCardAttributes + 1 (1 for the cardID)
    public String[] readCardWithID (int id){
        if (id <= 0) {//validate id
            return null;
        }
        
        String[] cardData = new String[numOfCardAttributes +1];
        
        int j=1;
        int startRecordId = this.convertToRecordId(id);
        int endRecordId = startRecordId + numOfCardAttributes;
        cardData[0] = String.valueOf(id);
        for (int i=startRecordId;i<endRecordId;i++){
            cardData[j] = readRecord(i);
            j++;
        }
        return cardData;
    }

    //return cards (note the 's') with its ID
    public String[][] readAllCards (){
        
        int numOfCards = this.numOfCards();
        int numOfCardAtt = this.numOfCardAttributes;
        String[][] cardData = new String[numOfCards][numOfCardAtt +1];//11+1, 1 for the ID
        int i=0;//index
        int cardID = 1;
        
        while (i < numOfCards){
            if (!isCardEmpty(cardID)){
                cardData[i] = this.readCardWithID(cardID);
                i++;
            }
            cardID++;
        }
        return cardData;
    }
    
    //CardId becomes recordId, e.g: 1 becomes 11, 2 becomes 22, and so on
    public int convertToRecordId(int cardId){
        return cardStartId + (cardId -1) * numOfCardAttributes;
    }
    
    public int convertToCardId(int recordId){
        if (recordId < cardStartId){
            return 0;
        }else if ((recordId - cardStartId) % numOfCardAttributes !=0 ){//check validity of recordId, e.g 12 should not be permited
            return 0;
        }else {
            return ((recordId - cardStartId)/numOfCardAttributes) + 1;
        }
        
    }
    
    /**
     * this method only use blankRecord, that means the slot or the record can be use for another card, later
     * unlike the delRecord that make the slot not available for addition/editing
     * @param cardId
     * @return true if works
     */
    
    public boolean deleteCard (int cardId){
        if (cardId <= 0){//there is no card with 0 as Id
            return false;
        }
        
        int startRecordId = convertToRecordId(cardId);
        int endRecordId = startRecordId + numOfCardAttributes;
        for (int i=startRecordId;i<endRecordId;i++){
            if (!blankRecord(i)){
                return false;
            }
        }

        //don't forget to -- numOfCards()
        int oldNumOfCards = numOfCards();
        if (oldNumOfCards > 0){
            this.saveRecord(4, String.valueOf(oldNumOfCards-1));
        }
        
        //add blankId to emptySlots
        addToEmptySlots(cardId);
        
        return true;
    }
    
    /** This method came from Mnemosyne's method with the same name
     * 
     * @param grade
     * @return int initial interval for particular grade
     */
    public int calculateInitialInterval(byte grade){
        int interval;
        switch (grade){
            case 2:
                interval = 1;
                break;
            case 3:
                interval = 3;
                break;
            case 4:
                interval = 4;
            case 5:
                interval = 5;
                break;
            default:
                interval = 0;
        }
        return interval;
    }
    

    //calculate interval noise to make intervals among the cards different
    public int calculateIntervalNoise (int interval){
        int noise;
        if (interval == 0){
            noise = 0;
        }else if(interval == 1){
            noise = getRand(0,1);
        }else if (interval <= 10){
            noise = getRand(-1,1);
        }else if (interval <= 60){
            noise = getRand(-3, 3);
        }else {
            double a = 0.05 * interval;
            int b = (int) a;
            noise = getRand(-b,b);
        }
        return noise;
    }
    
    //for getting a random number between (and including) $min and $max
    private static final Random randomizer = new Random();
    private static final int getRand(int min, int max){
        int rndmNumber = Math.abs(randomizer.nextInt());
        return (rndmNumber % ((max - min + 1))) + min;
    }
    
    //UNTESTED
    //return average of all Cards
    public double averageEasiness(){
        if (numOfCards() == 0){
            return 2.5;
        }else {
            int max = numOfCards();
            int i = 0;
            double sum = 0;
            int cardID = 1;
            while (i<max){
                if (!readRecord(convertToRecordId(cardID)).equals("")){//if a grade of a card is blank, it means it is an empty slot
                    sum += getEasinessOfcard(cardID);//plus 1 because easiness attribute is located in the 2nd field                    
                    i++;
                }
                cardID++;
            }
            //always round an easiness factor to 5 digits before it's saved
            return round(sum/max);
        }
        
    }
    
    //return number of Cards
    public int numOfCards() {
        int re = Integer.parseInt(this.readRecord(4));//see initiateData()
        return (int) re;
    }
    
    //get 'days' since startTime
    public int daysSinceStart(){
        int timeOfStart = getTimeOfStart();
        int now = (int) (System.currentTimeMillis() / 1000);
        return ((now - timeOfStart)/86400);
    }
    
    //get timeOfStart
    public int getTimeOfStart(){
        return Integer.parseInt(this.readRecord(1));//ID 1 contains data about timeOfStart, see initiateData()
    }
    
    public boolean isCardEmpty(int cardId){
        if (cardId <=0){
            return true;
        }else if ( readRecord(convertToRecordId(cardId)) == null ||
                readRecord(convertToRecordId(cardId)).equals("") 
                ){
            return true;
        }
        return false;
    }
    
    //UNTESTED
    /** Get content of particular field in card
     * 
     * @param cardId valid card Id
     * @param fpos field's position in the card.
     * Grade = 1, ac_rp = 3, rt_rp = 4, lapse = 5, ac_rp_r; = 6, rt_rp_rl = 7,
     * l_rp = 8, n_rp = 9
     * @return content of field in integer type
     */
    public int getFieldOfCard(int cardId, int fpos){
        return Integer.parseInt(readRecord(convertToRecordId(cardId)+fpos-1));
    }
    
    //UNTESTED
    public double getEasinessOfcard(int cardId){
        return Double.parseDouble(readRecord(convertToRecordId(cardId)+1));//1 is the position of easiness
    }
    
    //UNTESTED
    public String getQuestionOfCard(int cardId){
        return readRecord(convertToRecordId(cardId)+9);
    }
    
    public String getAnswerOfCard(int cardId){
        return readRecord(convertToRecordId(cardId)+10);
    }
    
    //UNTESTED
    /** Set content of particular field in card
     *  Unlike getFieldOfCard, this method uses String as a new content (more
     * universal)
     * 
     * @param cardId valid card Id
     * @param fpos field's position in the card.
     * Grade = 1, easiness = 2, ac_rp = 3,
     * rt_rp = 4, lapse = 5, ac_rp_r; = 6,
     * rt_rp_rl = 7, l_rp = 8, n_rp = 9,
     * question = 10, answer = 11
     * @param content new content
     * @return boolean
     */
    public boolean setFieldOfCard(int cardId, int fpos, String content){
        if (content.equals("")){
            return false;
        }
        
        saveRecord(convertToRecordId(cardId)+fpos-1, content);
        return true;
    }
    
    /** 
     * Used to round a double
     * Thanks to Eko Paris Besteriyana Yulianto
     * http://donturnaround.wordpress.com/
     * @param double
     * @return double
     */
    public double round(double number){
        int level = 100000;
        int temp1 = (int) Math.floor(number*level + 0.5);
        return (double) temp1/level;
    }
    
    //UNTESTED
    /** Get all of valid CardIDs
     * 
     * @return valid card Ids in Array of int
     */
    public int[] getCardIds(){
        int numOfCards = this.numOfCards();
        int[] cardIds = new int[numOfCards];
        
        int i=0;//index
        int cardId = 1;
        
        while (i < numOfCards){
            if (!isCardEmpty(cardId)){
                cardIds[i] = cardId;
                i++;
            }
            cardId++;
        }
        return cardIds;
    }
      
//Start from here, functions from main method (the rest mnemosyne_core), all are UNTESTEd
    public boolean resetLearningData(int cardId){
        int startRecId = convertToRecordId(cardId);
        saveRecord(startRecId, "0");//grade
        saveRecord(startRecId+1, "2.5");//easiness
        
        for (int i=2;i<=8;i++){
            saveRecord(startRecId+i, "0");
        }
        return true;
    }
    
    
    public int sortKey(int cardId){
        return Integer.parseInt((readRecord(convertToRecordId(cardId)+8)));//next_rep position
    }
    
    public int sortKeyInterval(int cardId){
        int n_rp = getFieldOfCard(cardId, 9);
        int l_rp = getFieldOfCard(cardId, 8);
        
        return (n_rp-l_rp);
    }
    
    public int sortKeyNewest(int cardId){
        int acq_reps = getFieldOfCard(cardId, 3);
        int ret_reps = getFieldOfCard(cardId, 4);
        return acq_reps+ret_reps;
    }
    
    public boolean isNew(int cardId){
        return (getFieldOfCard(cardId, 3) == 0) && (getFieldOfCard(cardId, 4) == 0);
    }
    
    public boolean isDueForAcquisitionRep(int cardId){
        return (getFieldOfCard(cardId, 1) < 2);
    }
    
    
    public boolean isDueForRetentionRep(int cardId, int days){//default for days = 0
        int grade = getFieldOfCard(cardId, 1);
        int next_rep = getFieldOfCard(cardId, 9);
        return (grade >=2) && (daysSinceStart() >= next_rep - days);
    }
    
    public boolean isOverDue(int cardId){
        int grade = getFieldOfCard(cardId, 1);
        int next_rep = getFieldOfCard(cardId, 9);
        return ( grade >=2 ) && ( daysSinceStart() > next_rep );
    }
    
    public int daysSinceLastRep(int cardId){
        int last_rep = getFieldOfCard(cardId, 8);
        
        return ( daysSinceStart()-last_rep );
    }
    
    public int daysUntilNextRep (int cardId){
        int next_rep = getFieldOfCard(cardId, 9);
        
        return ( next_rep - daysSinceStart() );
    }
    
    public boolean qualifiesForLearnAhead(int cardId){
        int grade = getFieldOfCard(cardId, 1);
        int next_rep = getFieldOfCard(cardId, 9);
        return ( grade >= 2 ) && ( daysSinceStart() < next_rep );
    }
    
    public int nonMemorisedItems(){
        int[] cardData = getCardIds();
        int sumOf = 0;
        for (int i=0;i<cardData.length;i++){
            if (isDueForAcquisitionRep(cardData[i])){
                sumOf++;
            }
        }
        return sumOf;
    }
    
    /** Get sum of scheduled items 
     * 
     * @param days, default is 0
     * @return sumOf scheduled items
     */
    public int scheduledItems(int days){//default of days = 0;
        int[] cardData = getCardIds();
        int max = cardData.length;
        int sumOf = 0;
        for (int i=0;i<max;i++){
            if (isDueForRetentionRep(cardData[i], days)){
                sumOf++;
            }
        }
        return sumOf;
    }
    
//ultimate method
    public Vector rebuildRevisionQueue(boolean learnAhead){//default for learAhead = false
        //Since there is no ArrayList in JME (and it's kind of wasting if using Array)
        //we use Vector
        Vector revisionQueue = new Vector();
        
        if (numOfCards() == 0)
            return revisionQueue;
        
        /** unnecessary since isDue*() always call daysSinceStart(). Not known
         *  which one is better
         */
        //int daysSinceStart = daysSinceStart();
        
        /**Do the cards that are scheduled for today (or are overdue), but 
         * first do those that have the shortest interal, as being a day 
         * late on an interval of 2 could be much worse than being a day late
         * on an interval of 50
         */
        
        int[] allCardIds = getCardIds();
        int numOfCards = allCardIds.length;
        for (int i=0;i<numOfCards;i++){
            if ( isDueForRetentionRep(allCardIds[i], 0) ){
                revisionQueue.addElement(String.valueOf(allCardIds[i]));
            }
        }
        
        //convert Vector into array, sort, convert back to Vector
        revisionQueue = arrayIntToVector(sort(vectorToArrayInt(revisionQueue), "SortKeyInterval"));
        
        if (revisionQueue.size() != 0){
            return revisionQueue;
        }
        
        /** Now memorise the cards that we got wrong during the last stage.
         *  Concentrate on only a limited number of grade 0 cards, in order to
         *  avoid too long intervals between revisions. If there are too few
         *  cards in left in the queue, appen dmore new cards to keep some
         *  spread between these last cards.
         * 
         */
        
        int limit = Integer.parseInt(readRecord(2));//2 is the position of 'grade0 items at once'-setting
        
        Vector grade0 = new Vector();
        /**
         * there are still values in allCardIds and numOfCards so we'll use it
         * Note: since there is no items_are_inverses(), so no need
         * to assign grade0Selected
         */
        for (int i=0;i<numOfCards;i++){
            if ( isDueForAcquisitionRep(allCardIds[i]) &&
                    getFieldOfCard(allCardIds[i], 5)>0 && //lapses > 0
                    getFieldOfCard(allCardIds[i], 1) == 0 //grade == 0
                ){
                grade0.addElement(String.valueOf(allCardIds[i]));
                if (grade0.size() >= limit){
                    break;//stop, no more than 'grade0 items at once'-setting
                }
            }
        }
        
        /**
         * Check for grade 1 in cards and add it to revisionQueue
         * Again, we use allCardIds and numOfCards
         */
        Vector grade1 = new Vector();
        for (int i=0;i<numOfCards;i++){
            if ( isDueForAcquisitionRep(allCardIds[i]) &&
                 getFieldOfCard(allCardIds[i], 5) > 0 &&//lapses > 0
                 getFieldOfCard(allCardIds[i], 1) == 1//grade == 1
                 ){
                grade1.addElement(String.valueOf(allCardIds[i]));
            }
        }
        
        /** Since there is no vector merge in JME (vector1.addAll(vector2))
         *  we use the conventional way
         */
        for (int i=0;i<grade0.size();i++){
            revisionQueue.addElement(grade0.elementAt(i));
            revisionQueue.addElement(grade0.elementAt(i));//still considering another approach..
        }
        for (int i=0;i<grade1.size();i++){
            revisionQueue.addElement(grade1.elementAt(i));
        }
        
        //shuffle vector's objects
        revisionQueue = shuffleVector(revisionQueue);
        
        if (grade0.size() >= limit || revisionQueue.size() >= 10 ){
            return revisionQueue;
        }
        
        /** Now do the cards which have never been committed to long-term memory,
         *  but which we have seen before
         */
        int grade0InQueue = grade0.size();
        grade0.removeAllElements();
        for (int i=0; i<numOfCards; i++){
            if (isDueForAcquisitionRep(allCardIds[i]) &&
                getFieldOfCard(allCardIds[i], 5) == 0 &&//lapses == 0
                getFieldOfCard(allCardIds[i], 3) > 1 &&//acq_reps > 1
                getFieldOfCard(allCardIds[i], 1) == 0//grade == 0
                ){
                grade0.addElement(String.valueOf(allCardIds[i]));
                //no more than limit (=grade 0 cards learn at once)
                if (grade0.size()+ grade0InQueue >= limit){
                    break;
                }
            }
        }
        
        for (int i=0; i<numOfCards; i++){
            if (isDueForAcquisitionRep(allCardIds[i]) &&
                getFieldOfCard(allCardIds[i], 5) == 0 &&//lapses == 0
                getFieldOfCard(allCardIds[i], 3) > 1 &&//acq_reps > 1
                getFieldOfCard(allCardIds[i], 1) == 1//grade == 1
                ){
                grade1.addElement(String.valueOf(allCardIds[i]));
            }
        }
        
        /** Since there is no vector merge in JME (vector1.addAll(vector2))
         *  we use the conventional way
         */
        for (int i=0;i<grade0.size();i++){
            revisionQueue.addElement(grade0.elementAt(i));
            revisionQueue.addElement(grade0.elementAt(i));//still considering another approach..
        }
        for (int i=0;i<grade1.size();i++){
            revisionQueue.addElement(grade1.elementAt(i));
        }
        
        //shuffle revisionQueue's objects
        revisionQueue = shuffleVector(revisionQueue);
        
        if (grade0.size() + grade0InQueue >= limit || revisionQueue.size() >= 10){
            return revisionQueue;
        }
        
        /** Now add some new cards. This is a bit inefficient at the moment as
         *  'unseen' is wastefully created as opposed to being a generator
         *  expression. However, in order to use random.choice, there doesn't
         *  seem to be another option.
         */
        Vector unseen = new Vector();
        for (int i=0;i<numOfCards;i++){
            if (isDueForAcquisitionRep(allCardIds[i]) &&
                getFieldOfCard(allCardIds[i], 3) <= 1//acq_reps <=1
                ){
                unseen.addElement(String.valueOf(allCardIds[i]));
            }
        }
        
        //grade_0_in_queue = sum(1 for i in revision_queue if i.grade == 0)/2
        grade0InQueue = 0;
        for (int i=0; i<revisionQueue.size(); i++){
            //if grade == 0
            if (getFieldOfCard(Integer.parseInt(String.valueOf(revisionQueue.elementAt(i))), 1) == 0){
                grade0InQueue++;
            }
        }
        grade0InQueue = grade0InQueue/2;
        
        //grade_0_selected = [] (since we ain't using grade_0_selected)
        grade0.removeAllElements();
        Vector newCard = new Vector();
        if (limit != 0 && unseen.size() != 0){
            while (true){
                //if get_config("randomise_new_cards") == False:
                if (readRecord(3).equals("0")){
                    newCard.addElement(unseen.elementAt(0));
                }else {
                    //random.choice(unseen)
                    newCard.addElement(unseen.elementAt(getRand(0, unseen.size()-1)));
                }
                
                unseen.removeElement(newCard.lastElement());
                
                //no items_are_inverses, so all newCard appended to grade0
                grade0.addElement(newCard.lastElement());
                
                if (unseen.size() == 0 ||
                    grade0.size() + grade0InQueue >= limit
                    ){
                    //manually add grade0 to revisionQueue
                    for (int i=0; i<grade0.size(); i++){
                        revisionQueue.addElement(grade0.elementAt(i));
                    }
                    return revisionQueue;
                }
            }
        }
        
        /**
         * If we get to here, there are no more scheduled cards or new cards to
         * learn. The user can signal that he wants to learn ahead by calling
         * rebuildRevisioQueue with 'learnAhead' set to True. Don't shuffle
         * this queue, as it's more useful to review earliest scheduled cards
         * first.
         */
        
        if (learnAhead == false){
            return revisionQueue;
        }else {
            revisionQueue.removeAllElements();
            for (int i=0; i<numOfCards; i++){
                if (qualifiesForLearnAhead(allCardIds[i])){
                    revisionQueue.addElement(String.valueOf(allCardIds[i]));
                }
            }
            revisionQueue = arrayIntToVector(sort(vectorToArrayInt(revisionQueue), "SortKey"));
        }
        return revisionQueue;
    }//end of rebuildRevisionQueue method
    
    public int processAnswer(int cardId, int newGrade){
        
        
        int newInterval;
        int noise;
        
        /**
         * everything's done to revQueue, should be assigned to revisionQueue
         * For now, just make a local var
         */
        Vector revQueue = Learn.getRevQueue();
        
        int oldGrade = getFieldOfCard(cardId, 1);//get previous grade of card
        
        /**
         * Calculate scheduled and actual interval, taking care of corner
         * case when learning ahead on the same day.
         */
        int scheduledInterval = getFieldOfCard(cardId, 9) - getFieldOfCard(cardId, 8);
        int actualInterval = daysSinceStart() - getFieldOfCard(cardId, 8);
        
        if (actualInterval == 0){
            actualInterval = 1;
        }
        
        if (isNew(cardId)){
            // The item is not graded yet, e.g. because it is imported.
            setFieldOfCard(cardId, 3, "1");//item.acq_reps = 1
            setFieldOfCard(cardId, 6, "1");//item.acq_reps_since_lapse = 1
            
            newInterval = calculateInitialInterval((byte) newGrade);
            
            //Make sure the second copy of a grade 0 item doesn't show up again
            if (oldGrade == 0 &&
                (newGrade == 2 || newGrade == 3 ||
                newGrade == 4 || newGrade == 5)
                ){
                revQueue.removeElement(String.valueOf(cardId));
            }
            
        }else if ((oldGrade == 0 || oldGrade == 1) &&
                  (newGrade == 0 || newGrade == 1) 
                ){
            // In the acquisition phase and staying there
            setFieldOfCard(cardId, 3, String.valueOf(getFieldOfCard(cardId, 3)+1));//acq_reps++
            setFieldOfCard(cardId, 6, String.valueOf(getFieldOfCard(cardId, 6)+1));//acq_reps_l++
            
            newInterval = 0;
            
        }else if ( ( oldGrade == 0 || oldGrade == 1 ) &&
                   (newGrade == 2 || newGrade == 3 || newGrade == 4 || newGrade == 5)
                ){
            // In the acquisition phase and moving to the retention phase
            setFieldOfCard(cardId, 3, String.valueOf(getFieldOfCard(cardId, 3)+1));//acq_reps++
            setFieldOfCard(cardId, 6, String.valueOf(getFieldOfCard(cardId, 6)+1));//acq_reps_l++
            
            newInterval = 1;
            
            //Make sure the second copy of a grade 0 item doesn't show up again
            if ( oldGrade == 0 ){
                revQueue.removeElement(String.valueOf(cardId));
            }
            
        }else if ( (oldGrade == 2 || oldGrade == 3 || oldGrade == 4 || oldGrade == 5) &&
                   (newGrade == 0 || newGrade == 1)
                ){
            /**
             * In the retention phase and dropping back to the acquisition phase
             * item.ret_reps += 1
             * item.lapses += 1
             * item.acq_reps_since_lapse = 0
             * item.ret_reps_since_lapse = 0
             */ 
            
            setFieldOfCard(cardId, 4, String.valueOf(getFieldOfCard(cardId, 4)+1));
            setFieldOfCard(cardId, 5, String.valueOf(getFieldOfCard(cardId, 5)+1));
            setFieldOfCard(cardId, 6, "0");
            setFieldOfCard(cardId, 7, "0");
            
            newInterval = 0;
            
            /** Move this item to the front of the list, to have precedence over
             * items which are still being learned for the first time
             */
            revQueue.removeElement(String.valueOf(cardId));
            revQueue.insertElementAt(String.valueOf(cardId), 0);
        }else /*if ( (oldGrade == 2 || oldGrade == 3 || oldGrade == 4 || oldGrade == 5) &&
                   (newGrade == 2 || newGrade == 3 || newGrade == 4 || newGrade == 5)
                )*/{
            /** In the retention phase and staying there.
             * item.ret_reps += 1
             * item.ret_reps_since_lapse += 1 
             */
            setFieldOfCard(cardId, 4, String.valueOf(getFieldOfCard(cardId, 4)+1));
            setFieldOfCard(cardId, 7, String.valueOf(getFieldOfCard(cardId, 7)+1));
            
            //asign an easiness value (fpos = 2)
            if ( actualInterval >= scheduledInterval ){
                if ( newGrade == 2 ){
                    setFieldOfCard(cardId, 2, String.valueOf(getEasinessOfcard(cardId)-0.16));
                }else if ( newGrade == 3){
                    setFieldOfCard(cardId, 2, String.valueOf(getEasinessOfcard(cardId)-0.14));
                }else if ( newGrade == 5 ){
                    setFieldOfCard(cardId, 2, String.valueOf(getEasinessOfcard(cardId)+0.10));
                }
                if ( getEasinessOfcard(cardId) < 1.3 ){
                    setFieldOfCard(cardId, 2, "1.3");
                }
            }
            
            newInterval = 0;//initialize
            
            if ( getFieldOfCard(cardId, 7) == 1 ){//ret_reps_since_lapse
                newInterval = 6;
            }else {
                if ( newGrade == 2 || newGrade == 3 ){
                    if ( actualInterval <= scheduledInterval ){
                        newInterval = actualInterval * (int) getEasinessOfcard(cardId);
                    }else {
                        newInterval = scheduledInterval;
                    }
                }
                
                if ( newGrade == 4 ){
                    newInterval = actualInterval * (int) getEasinessOfcard(cardId);
                }
                
                if ( newGrade == 5 ){
                    if ( actualInterval < scheduledInterval ){
                        newInterval = scheduledInterval;//Avoid spacing
                    }else {
                        newInterval = actualInterval * (int) getEasinessOfcard(cardId);
                    }
                }
            }
            
            // Shouldn't happen, but build in a safeguard
            if ( newInterval == 0 ){
                newInterval = scheduledInterval;
            }
            
            
             
        }//end of if-else oldGrade-newGrade
        
        //Add some randomness to interval
        noise = calculateIntervalNoise(newInterval);

        //Update grade and interval
        setFieldOfCard(cardId, 1, String.valueOf(newGrade));//item.grade = newGrade;
        setFieldOfCard(cardId, 8, String.valueOf(daysSinceStart()));//last repetition = today
        setFieldOfCard(cardId, 9, String.valueOf(daysSinceStart() + newInterval + noise));

        //<strike>Don't schedule inverse or identical questions on the same day</strike
        //Since we don't check for duplication or inverses item
        
        //set what happened to revQueue on this method to Learn class' revisionQueue
        Learn.setRevQueue(revQueue);
        
        return newInterval + noise;
    }
    
    /** 
     * Check if a card's already in queue
     * 
     * @param vector
     * @param cardId
     * @return true if cardId is in vector, otherwise false
     */
    public boolean inRevisionQueue(Vector vector, int cardId){
        return vector.contains(String.valueOf(cardId));
    }
    
    /** Dump functions goes here...
     */
    /**convert a vector into array of integer
     * 
     * @param vector
     * @return aray of integer
     */
    public int[] vectorToArrayInt (Vector vector){
        int vectorSize = vector.size();
        int[] array = new int[vectorSize];

        for (int i=0;i<vectorSize;i++){
            array[i] = Integer.parseInt((String) vector.elementAt(i));
        }
        return array;
    }
    
    /** Convert an array of integer into vector
     * 
     * @param arrayOfInt
     * @return vector
     */
    public Vector arrayIntToVector (int[] arrayOfInt){
        Vector vector = new Vector();
        for (int i=0;i<arrayOfInt.length;i++){
            vector.addElement(String.valueOf(arrayOfInt[i]));
        }
        
        return vector;
    }
    
    /** Sort an array of integer
     * 
     * @param array
     * @param key, could be one of available functions (sortKeyInterval, sortKeyNewest, sortKey
     * @return sorted array of integer
     */
    public int[] sort(int[] cards, String function){
        /** SPECIAL NOTES
         *  Since this function takes 3 different values from 3 different methods,
         *  still considering which is the optimum. Another option is call the
         *  method each time in compare-section in the sort block, and no need to
         *  assign new array of integer variable (returnedValueFromFunction). Even
         *  for the worst case, the difference is not much significant.
         */
        int[] returnedValueFromFunction = new int[cards.length];
        
        if ( function.equals("sortKeyInterval")){
            for (int i=0;i<returnedValueFromFunction.length;i++){
                returnedValueFromFunction[i] = sortKeyInterval(cards[i]);
            }
        }else if ( function.equals("sortKeyNewest") ) {
            for (int i=0;i<returnedValueFromFunction.length;i++){
                returnedValueFromFunction[i] = sortKeyNewest(cards[i]);
            }
        }else if ( function.equals("sortKey") ){
            for (int i=0;i<returnedValueFromFunction.length;i++){
                returnedValueFromFunction[i] = sortKey(cards[i]);
            }
        }
        
        
        //start sorting based on Gosling's (http://www.cs.ubc.ca/~harrison/Java/BubbleSort2Algorithm.java)
        for ( int i = cards.length; --i>=0; ) {
            boolean flipped = false;
            for (int j = 0; j<i; j++) {
                //compare-section
                if ( returnedValueFromFunction[j] > returnedValueFromFunction[j+1] ) {
                    int T = cards[j];
                    cards[j] = cards[j+1];
                    cards[j+1] = T;
                    //also, swap the returnedValueFromFunction
                    int T2 = returnedValueFromFunction[j];
                    returnedValueFromFunction[j] = returnedValueFromFunction[j+1];
                    returnedValueFromFunction[j+1] = T2;
                    
                    flipped = true;
                }
            }
            if (!flipped) {
                return cards;
            }
        }
        
        return cards;
     
    }//end of method sort
    
    /** Again, no Vector.shuffle() in JME. Got a way from Gareth Maguire's blog
     *  http://garethmaguire.net/Blog/post/Java-ShuffleVector.aspx
     */
    public Vector shuffleVector(Vector vector){
        int index, randomIndex;
        Object tempObject;
        
        Random random = new Random();
        for (index =1; index < vector.size(); index++){
            randomIndex = random.nextInt(index+1);
            tempObject = vector.elementAt(index);
            vector.setElementAt(vector.elementAt(randomIndex), index);
            vector.setElementAt(tempObject, randomIndex);
        }
        
        return vector;
    }
    
}//end of class store
