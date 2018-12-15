#include <TimerOne.h>


int IR_PIN = 8;
int R_PIN = 3;
int G_PIN = 4;
int Y_PIN = 5;


int state = -1;
byte data = 0;

bool RedOn;
bool Flash;

int count;

void setup()
{
  Timer1.initialize(100000); 
  Timer1.attachInterrupt( timerIsr ); // attach the service routine here
//  Timer1.stop;
  RedOn=false;
  Flash=false;
  pinMode(IR_PIN,INPUT_PULLUP); // TSOP is connected on the 15ht pin  
  
  pinMode(R_PIN, OUTPUT);  
  pinMode(G_PIN, OUTPUT);  
  pinMode(Y_PIN, OUTPUT);
  
  digitalWrite(R_PIN, LOW);
  digitalWrite(G_PIN, LOW);
  digitalWrite(Y_PIN, LOW);    
  Serial.begin(9600);
//  count = 0;
}

void timerIsr()
{
  if ((~RedOn)&Flash) {
    count=count+1;
    if (count==2) {
      digitalWrite(R_PIN,LOW);
      Flash=false;
    }
  }
//  Timer1.stop;
}
void loop() 
 {  
  int remote_val = remote();  
  if(remote_val>0)  
  {  
   Serial.println(remote_val, HEX);  
   if (~RedOn) {
        digitalWrite(R_PIN,HIGH);
        Flash=true; 
        count=0;
       }          

//   count = count+1;
 //  Serial.print(" Count : ");
 //  Serial.println(count);  
 // if (count == 10)  digitalWrite(R_PIN, HIGH);
  }
}

int remote()  
 {  
  int value = 0;  
  int time = pulseIn(IR_PIN,LOW);  
  if(time>2000) // Checking if the Start Bit has been received. Start Bit Duration is 2.4ms  
  {
//   Serial.println("Start");  
   for(int counter1=0;counter1<12;counter1++) // A loop to receive the next 12 bits  
   {  
    if(pulseIn(IR_PIN,LOW)>1000) // checking the duration of each pulse, if it is a '1' then we use it in our binary to decimal conversion, '0's can be ignored.  
    {  
//     Serial.println("1"); 
     value = value + (1<< counter1);// binary to decimail conversion. 1<< i is nothing but 2 raised to the power of i  
    }  
   }  
  }  
  return value;  
 } 
 
void serialEvent() {
  while (Serial.available()) {
    // получаем новый байт:
    char inChar = (char)Serial.read(); 
     switch (inChar) {
      case 'R':
        state=0;
        break;
      case 'G':
        state=1;
        break;
      case 'Y':
        state=2;
        break;
      case '1':
        if ((state!=-1)&(state<10)) state=10+state;    
        break;
      case '0':
        if ((state!=-1)&(state<10)) state=20+state;
        break;
      case 'F':
        if (~RedOn) {
             digitalWrite(R_PIN,1);
             Flash=true; 
             count=0;
            }          
        break;        
      default:
        // turn all the LEDs off:
        state=-1;
    }
    
   
  switch (state){
      case 10:
//        Serial.println("Red On"); 
        Serial.println("R1OK"); 
        data=data | B00000001;
        digitalWrite(R_PIN, HIGH);
        state=-1;
        RedOn=true;
        break;
      case 11:
//        Serial.println("Green On"); 
        Serial.println("G1OK"); 
        data=data | B00000010;
        digitalWrite(G_PIN, HIGH);
        state=-1;
        break;
      case 12:
//        Serial.println("Yellow On"); 
        Serial.println("Y1OK"); 
        data=data | B00000100;
        digitalWrite(Y_PIN, HIGH);
        state=-1;
        break;
      case 20:
//        Serial.println("Red Off"); 
        Serial.println("R0OK"); 
        data=data & B11111110;
        digitalWrite(R_PIN, LOW);
        state=-1;
        RedOn=false;
        break;
      case 21:
//        Serial.println("Green Off"); 
        Serial.println("G0OK"); 
        data=data & B11111101;
        digitalWrite(G_PIN, LOW);
        state=-1;
        break;
      case 22:
//        Serial.println("Yellow Off"); 
        Serial.println("Y0OK"); 
        data=data & B11111011;
        digitalWrite(Y_PIN, LOW);
        state=-1;
        break;
      }   
//    Serial.println(data); 
    
  }
}
