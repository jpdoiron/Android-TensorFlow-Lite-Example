package com.amitshekhar.tflite;

class Box {
    public String toString(){
        String s = "p:" + Probability + " : " + x1 + "," + y1 + "," + x2 + "," + y2;
        return s;
    }

    float Probability  = 0.0f;
    int x1 = 0;
    int y1 = 0;
    int x2 = 0;
    int y2 = 0;

    public Box getScaledBox(int width, int height)
    {
        Box b= new Box();

        b.x1 = (int)((x1 /224.0) * width );
        b.x2 = (int)((x2 /224.0) * width );
        b.y1 = (int)((y1 /224.0) * height);
        b.y2 = (int)((y2 /224.0) * height);
        return b;
    }

}