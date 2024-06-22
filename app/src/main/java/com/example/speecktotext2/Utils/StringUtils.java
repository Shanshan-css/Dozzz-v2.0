package com.example.speecktotext2.Utils;

public class StringUtils {
    String receive = "";
    RegexSearch rs = new RegexSearch();
    public static String[][] sSendTo = new String[][]{
            {"don't understand", "repeat again", "can't understand", "say again", "sorry"},
            {"yes", "good", "great", "fine", "okay", "nice","ready"},
            {"no","not too well","not at all", "sad", "angry", "not that great", "not really", "not good", "not that much", "something else", "don't know"},
            {"ready","now"},
            {"no time","busy"},
            {"parent","mom","dad","parents","brother","sister","own bed","grandma","grandpa","mother","father"},
            {"\\d{1,}"},
            {"see you","bye","by","buy","goodbye"},
            {"hello","morning","evening","afternoon","hi"}
    };

    public static String[][] sSendReceived = new String[][]{
            {"Hi my friend! Emma speaking!", "Do you have some time now to talk about your sleep?"},
            {"Did you sleep well?"},
            {"Did you watch a lot of tv or played video games yesterday?"},
            {"Do you know why you have slept bad?"},
            {"What did you eat and drink yesterday?"},
    };
    public static String[][] sSendReceivedYes = new String[][]{
            {"Nice to get to know you!","How are you feeling today?"},
            {"Good to hear!","In which bed have you slept last night?"},
            {"Nice to hear!", "Do you know why you have slept good?"},
            {"Lovely to hear, honey!", "Did you watch a lot of tv or played video games yesterday?"},
            {"","How many minutes did you watch tv, or played video games yesterday?"},
            {"Sounds good!","Are you going to do something nice and exciting today?"},
            {"Sounds good!"," Where do you need to go today?"},
            {"Good to hear!","What have you planned for today?"},
            {"","Okay! Good luck today and have fun! I will see you tomorrow!"}
    };
    public static String[][] sSendReceivedNo = new String[][]{
            {"","Ok! Then I will see you soon! Please let me know when you are ready"},
            {"Oh No! Hope you feel better soon!","In which bed have you slept last night?"},
            {"Oh No!","Do you know why you have slept bad?"},
            {"Oh no!", "Did you watch a lot of tv or played video games yesterday?"},
            {"","What did you eat and drink yesterday?"},
            {""},
            {"Oh no!","What is something you like to do?"},
            {"That makes me sad!","Did maybe something happen yesterday? Or do you need to do something not so nice today?"},
            {"Oh no!","What is something you like to do?"},
            {"","Okay! Good luck today and have fun! I will see you tomorrow!"}
    };

    //第一次提问，输出中性的所有问题
    public String question(int k) {
        String receive = "";
        for (int i = 0; i < sSendReceived[k].length; i++) {
            receive += sSendReceived[k][i];
        }
        return receive;
    }
    //第一次提问，输出肯定的所有问题
    public String questionYes(int k) {
        String receive = "";
        for (int i = 0; i < sSendReceivedYes[k].length; i++) {
            receive += sSendReceivedYes[k][i];
        }
        return receive;
    }
    //第一次提问，输出否定的所有问题
    public String questionNo(int k) {
        String receive = "";
        for (int i = 0; i < sSendReceivedNo[k].length; i++) {
            receive += sSendReceivedNo[k][i];
        }
        return receive;
    }

    //匹配没有时间的问题
    public boolean questionNoTime(String answers){
        boolean search5 = false;
        for (int j = 0; j < sSendTo[4].length; j++) {
            String pattern5 = sSendTo[4][j];
            search5 = rs.searchOrNot(answers, pattern5);
            if (search5) {
                break;
            }
        }
        return search5;
    }

    //匹配有时间
    public boolean questionHasTime(String answers){
        boolean search4 = false;
        for (int j = 0; j < sSendTo[3].length; j++) {
            String pattern5 = sSendTo[3][j];
            search4 = rs.searchOrNot(answers, pattern5);
            if (search4) {
                break;
            }
        }
        return search4;
    }

    //匹配没听懂
    public boolean searchMatchSorry(String answers) {
        boolean search1 = false;
        for (int j = 0; j < sSendTo[0].length; j++) {
            String pattern1 = sSendTo[0][j];
            search1 = rs.searchOrNot(answers, pattern1);
            if (search1) {
                break;
            }
        }
        return search1;
    }
    //匹配人
    public boolean searchMatchPeo(String answers) {
        boolean search6 = false;
        for (int j = 0; j < sSendTo[5].length; j++) {
            String pattern5 = sSendTo[5][j];
            search6 = rs.searchOrNot(answers, pattern5);
            if (search6) {
                break;
            }
        }
        return search6;
    }
    //匹配数字
    public boolean searchMatchNum(String answers) {
        boolean search7 = false;
        for (int j = 0; j < sSendTo[6].length; j++) {
            String pattern6 = sSendTo[6][j];
            search7 = rs.searchOrNot(answers, pattern6);
            if (search7) {
                break;
            }
        }
        return search7;
    }
    //匹配结束
    public boolean searchMatchEnd(String answers) {
        boolean search8 = false;
        for (int j = 0; j < sSendTo[7].length; j++) {
            String pattern7 = sSendTo[7][j];
            search8 = rs.searchOrNot(answers, pattern7);
            if (search8) {
                break;
            }
        }
        return search8;
    }
    //匹配开始
    public boolean searchMatchStart(String answers) {
        boolean search9 = false;
        for (int j = 0; j < sSendTo[8].length; j++) {
            String pattern8 = sSendTo[8][j];
            search9 = rs.searchOrNot(answers, pattern8);
            if (search9) {
                break;
            }
        }
        return search9;
    }
    //匹配肯定回答
    public boolean searchMatchYes(String answers) {
        boolean search2 = false;
        for (int j = 0; j < sSendTo[1].length; j++) {
            String pattern2 = sSendTo[1][j];
            search2 = rs.searchOrNot(answers, pattern2);
            if (search2) {
                break;
            }
        }
        return search2;
    }

    //匹配否定回答
    public boolean searchMatchNo(String answers) {
        boolean search3 = false;
        for (int j = 0; j < sSendTo[2].length; j++) {
            String pattern3 = sSendTo[2][j];
            search3 = rs.searchOrNot(answers, pattern3);
            if (search3) {
                break;
            }
        }
        return search3;
    }

    //被问没有听懂时，输出最后一个问题
    public String lastQuestion(int l) {
        String receive = "";
        receive = sSendReceived[l][sSendReceived[l].length - 1];
        return receive;
    }
}
