/*---- UTILS ----*/
#define DEBUG false
void blink( int times ) {
  pinMode(13, OUTPUT);
  for(int i = 0; i<times; i++) {
    digitalWrite(13, HIGH);
    delay(100);
    digitalWrite(13, LOW);
    delay(100);
  }
}

void(* resetFunc) (void) = 0; //function resets sketch (akin to hitting reset button)
bool compare(char const *cont, char const *value) { return strlen(cont) >= strlen(value) && strncmp(cont, value, strlen(value)) == 0; }

/*---- UTILS ----*/



//Serial communication definitions
#define CMDLENGTH  50   //The maximum length of a command. Changing this should be legal
char const *DELINEATOR = "END";       //I'm using charlist pointers instead of definitions so there is only one instance of every immutable string
char const *IDSTRING = "LangmuirBot"; //as opposed to dozens of literals sprinkled in. I have no idea if this makes a difference
char const *IDREQUEST = "IDR";
//char const *STOP = "STP"; //unused
//char const *CLOSE = "CLS";
char const *NUDGE = "NDG";
char const *REBOOT = "REBOOT";

//Stepper pins, values, variables, definitions
#define enaPin1 11
#define stepPin1 12
#define dirPin1 13

#define enaPin2 8
#define stepPin2 9
#define dirPin2 10

#define DELAY 1000


int RotationPinSet[] = { enaPin2, stepPin2, dirPin2 };
int PositionPinSet[] = { enaPin1, stepPin1, dirPin1 };
void setup() { /* Set pins as output */
  pinMode(enaPin1, OUTPUT);
  pinMode(stepPin1, OUTPUT);
  pinMode(dirPin1, OUTPUT);

  pinMode(enaPin2, OUTPUT);
  pinMode(stepPin2, OUTPUT);
  pinMode(dirPin2, OUTPUT);

  //disable stepper boards
  digitalWrite(enaPin1, HIGH);
  digitalWrite(enaPin2, HIGH);

  Serial.begin(9600);
}

void loop() {  /* Main program */
  readSerial();
  delay(5);
}

bool rotationPrecision = false;
int rotationSteps = 0;
#define rotationMin -PI
#define rotationMax PI

//conversions
#define StepsPerRadian 18.53
int rotationToSteps(float theta) { return floor(theta * StepsPerRadian); }
float stepsToRotation(int steps) { return ((float)steps) / StepsPerRadian ; }

void setRotationSteps(int targetSteps) {
  driveStepper(targetSteps - rotationSteps,RotationPinSet, !rotationPrecision); //drive stepper to targetSteps
  rotationSteps = targetSteps; //reset position
  
  writeSerialString("UPDATErot" + String( stepsToRotation(rotationSteps) ) );
}

void setRotation(float theta) { setRotationSteps( rotationToSteps( constrain( theta, rotationMin, rotationMax ) ) );}
void nudgeRotationUp() { setRotationSteps( rotationSteps + 1 ); }
void nudgeRotationDown() { setRotationSteps( rotationSteps - 1 ); }

bool positionPrecision = false;
int positionSteps = 0;
#define positionMin -15
#define positionMax 15

//conversions
#define StepsPerMM 24.8
int positionToSteps(float x) { return floor( x * StepsPerMM ); }
float stepsToPosition(int steps) { return ((float)steps) / StepsPerMM; }

void setPositionSteps(int targetSteps) {
  driveStepper(targetSteps - positionSteps, PositionPinSet, !positionPrecision); //drive stepper to targetSteps
  positionSteps = targetSteps; //reset position
  
  writeSerialString("UPDATEpos" + String( stepsToPosition(positionSteps) ) );
}
void setPosition(float x) { setPositionSteps( positionToSteps( constrain( x, positionMin, positionMax ) ) ); }
void nudgePositionUp() { setPositionSteps( positionSteps+1 ); }
void nudgePositionDown() { setPositionSteps( positionSteps-1); }






void handleBuffer(char *command) {
  char *cont;
  int cmdLength = strlen(command);

  // check here for malformed command
  if (cmdLength < 3) return; //emit error

  char cmd[4];
  strncpy (cmd, command, 3);
  cmd[3] = 0; //charlists must end with 0 to be considered strings

  //parses GET and SET commands and does things with them
  cont = command + 3;
  int contLength = strlen(cont);


  if ( DEBUG ) {
    writeSerial("cmd:  ");
    writeSerialln(cmd);
    writeSerial("cont: ");
    writeSerialln(cont);
  }



  if ( compare( cmd, IDREQUEST ) ) { //handshake
    writeSerial(IDREQUEST);
    writeSerialln(IDSTRING);
  } /*else if ( compare( cmd, CLOSE) ) { //This can be done with a few other commands
    //default positions better be 0 <-----------------------------------------!!!!
    setRotation(0);
    setPosition(0);
    writeSerialln(CLOSE);

    resetFunc(); //kill sketch
    
  }*/ else if ( compare( command, REBOOT) ) {
    writeSerialln(REBOOT);
    resetFunc();          //kill sketch, stop everything
  } /*else if ( compare( cmd, STOP) ) {   //Turns out this would take a lot of rejiggering to make this 'ESTOP' feature actually work-- not quite comfortable doing that right now
    writeSerialln(STOP);
    interrupt = true;     //interrupt whatever is moving
  }*/ else if ( compare( cmd, NUDGE) ) {
    if (compare(cont, "pos") ) {
      positionPrecision = true;
      if (compare(cont+3, "down")) nudgePositionDown();
      else if (compare(cont+3, "up")) nudgePositionUp();
    } else if (compare(cont, "rot") ) {
      rotationPrecision = true;
      if (compare(cont+3, "down")) nudgeRotationDown();
      else if (compare(cont+3, "up")) nudgeRotationUp();
    }
  } else if ( compare( cmd, "SET") ) {
    if ( compare(cont, "pos") ) {
      positionPrecision = false;
      int x = atoi(cont + 3); //3 is the length of "pos"-- skip that bit of array
      writeSerialln("SETpos"); //acknowledge command
      setPosition(x);
    } else if ( compare(cont, "rot") ) {
      rotationPrecision = false;
      float theta = (float) atof(cont + 3); //3 is the length of "rot"-- skip that bit of array
      writeSerialln("SETrot"); //acknowledge command
      setRotation(theta);
    }
  }
}




//misc underlying processes
void driveStepper(int steps, int pinSet[], bool disable) {
  int ena = pinSet[0];
  int step = pinSet[1];
  int dir = pinSet[2];

  //set direction
  if (steps < 0) {
    digitalWrite(dir, LOW);
    steps = -steps;
  } else digitalWrite(dir, HIGH);

  //enable stepper controller board
  digitalWrite( ena, LOW );
  for (int i = 0; i < steps; i++) {
    digitalWrite(step, HIGH);
    delayMicroseconds(DELAY);
    digitalWrite(step, LOW);
    delayMicroseconds(DELAY);
  }
  //disable stepper board (and stop drawing power)
  if ( disable ) digitalWrite( ena, HIGH );
}


/* Serial in and out methods -- these are the only methods that should touch the Serial object */

void writeSerial(char const *message) { Serial.print(message); }
void writeSerialln(char const *message) {
  Serial.print(message);
  Serial.print(DELINEATOR);

  if ( DEBUG ) Serial.println();
}
void writeSerialString(String message) {
  Serial.print(message);
  Serial.print(DELINEATOR);

  if ( DEBUG ) Serial.println();
}
void readSerial() {
  static char buffer[CMDLENGTH + 1]; //only allocated once. like a global variable, but scope is limited to this function
  
  int num = 0; //collects serial messages from the hardware buffer
  do {
    while (Serial.available() > 0) {
      buffer[num++] = Serial.read(); //write to position num
      buffer[num] = 0; //clear next position
    }

    /*if (num > 0 && DEBUG) {
      writeSerialString("got " + String(num)); //dangerous?
      writeSerialln(buffer);
    }*/
    
  } while ( strstr(buffer, DELINEATOR) == NULL );

  // got message, let's parse
  char *sCommand = buffer;
  char *sEnd = strstr(sCommand, DELINEATOR);
  
  while (sEnd != NULL) {
    // found at least one more command
    // terminate command string at "END" (DELINEATOR)
    *sEnd = 0;

    if ( DEBUG ) {
      writeSerialln( "complete command" );
    }

    // execute command
    handleBuffer(sCommand);

    // advance both pointers to next possible command
    sCommand = sEnd + 3;
    sEnd = strstr(sCommand, DELINEATOR);
  }
}
