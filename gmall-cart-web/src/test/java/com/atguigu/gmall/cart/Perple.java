package com.atguigu.gmall.cart;

class People {
    String name;
    public People(){
        System.out.print(1);
    }
    public People(String name){
        System.out.print(2);
        this.name = name;
    }
}
class Child extends People{
    People father;
    public Child(String name){
        System.out.print(3);
        this.name = name;
        father = new People(name + ":f");
    }
        public Child(){
        System.out.print(4);
        }
}

class s{
    public static void main(String[] args) {
        new Child("makie");
    }
}