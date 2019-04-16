package com.atguigu.gmall.manage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Testtest {
    @Test
    public void t(){
        String a = "asdfasgasdd";
        char[] chars = a.toCharArray();
        char a1 =chars[chars.length-1];


       char[] b = {a1};
        String c = b.toString();

        String s = a.replaceFirst(c, "L");

        System.out.print(s);
    }
    @Test
    public void test1(){

        int a = 5;
        int b = 2;
        double c = a/b;
        System.out.print(c);
    }
    @Test
    public  void Test2(){
        String s1 = "Please_Input_String";
        String substring = s1.substring(7).substring(0, 5);
        System.out.print(substring + "..." + s1);



    }
/*
    public Map<String,List<User>> spuListClass(List<User> usrList){

        Map<String, List<User>> map = new HashMap<>();

     

        for (int i = 0; i <usrList.size() ; i++) {

            List<Object> objects = new ArrayList<>();
            for (int j = 0; j < ; j++) {
                
            }

            map.put(User.classId,usrList);
        }
        return map;
    }*/
@Test
public void ttt(){
    List<User> userList = new ArrayList<>();
    User user = new User();
    for (int i = 0; i < 3; i++) {
        user.setUserID(i);
        userList.add(user);
        
    }
    for (User tem : userList) {
        System.out.println(tem.toString());

    }
}
   
}
class User{
    int userID;
    public int getUserID(){
        return userID;
    }
    public void setUserID(int userID){
        this.userID=userID;
    }

    @Override
    public String toString() {
        return String.valueOf(userID);
    }
}
