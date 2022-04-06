package com.company;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        Scanner scannerInput = new Scanner(System.in);
        System.out.print("Input File Name (e.g. input.txt): ");
        String fileName = scannerInput.nextLine();
        System.out.print("Byte Ordering: ");
        char byteType = scannerInput.nextLine().charAt(0);
        System.out.print("Floating Point Size: ");
        int flPointSize = scannerInput.nextInt();

        File inputFile = new File(fileName);
        File outputFile = new File("output.txt");
        FileWriter outputWrite = new FileWriter(outputFile);

        Scanner scannerFile = new Scanner(inputFile);

        while(scannerFile.hasNext()){
            String number = scannerFile.nextLine();
            int[] answer = number.contains(".") ? floatToBit(Double.parseDouble(number), flPointSize) : integerToBit(number, 16, true);
            String hex = endian(bitToHex(answer), byteType);
            outputWrite.write(hex);
            outputWrite.write(System.lineSeparator());
        }
        outputWrite.close();
    }

    public static int stringToInt(String numberString) {
        return numberString.charAt(numberString.length() - 1) == 'u' ? Integer.parseInt(numberString.substring(0, numberString.length() - 1)) : Integer.parseInt(numberString);
    }
    
    //If fixedSize is only called from fraction part then function is false.
    //The purpose of the int signed part is when sent 16-bit unsigned or signed, if it is unsigned, it can use all bits.
    //But if it is signed, the first bit should be reserved for signed, so the for loop should start from the 2nd bit from the last (ie 1st index from the last).
    //Since this event is not valid for those coming from fraction part, so we added the fixedSize boolean.
    public static int[] integerToBit(String numberString, int size, boolean fixedSize) {
        int[] answer = new int[size];
        int number = stringToInt(numberString);
        int signed = numberString.charAt(numberString.length() - 1) == 'u' || !fixedSize ? 0 : 1;

        boolean negative = number < 0;
        number = Math.abs(number);

        for(int i = size - signed; i >= 0; i--) {
            if (Math.pow(2, i) <= number) {
                number -= Math.pow(2, i);
                answer[size - 1 - i] = 1;
            }
        }

        if (negative) translateToNegative(answer);
        return answer;
    }
    
    //This code doesn't do anything until sees the first 1, after seeing it inverst every bit.
    public static void translateToNegative(int[] answer) {
        boolean firstSeen = false;
        for(int i = 15; i >= 0; i--) {
            if (firstSeen) answer[i] = answer[i] == 1 ? 0 : 1;
            if (answer[i] == 1 && !firstSeen) firstSeen = true;
        }
    }
    
    //The function that converts the incoming bit array to hexa, regardless of the length of the bit.
    public static String bitToHex(int[] bits) {
        StringBuilder answer = new StringBuilder();
        for(int k = 1; k <= bits.length / 4; k++) {
            int hexValue = 0;
            for(int i = 4*k - 1; i >= 4*k - 4; i--) {
                if (bits[i] == 1) hexValue += (i >= 4) ? Math.pow(2, 3 - (i % 4)) : Math.pow(2, 3 - i);
            }
            answer.append(hexValue < 10 ? hexValue : String.valueOf(Character.toChars((hexValue - 10) + 'A')));
        }
        return answer.toString();
    }
    
    //If endian is b, function leaves it. Just adds a space every 2 characters.
    //If it is 1 then invert.
    public static String endian(String hex, char endian) {
        StringBuilder newHex = new StringBuilder();

        if (endian == 'l') {
            for (int i = hex.length() - 2; i >= 0; i -= 2) {
                newHex.append(hex.charAt(i));
                newHex.append(hex.charAt(i + 1));
                newHex.append(" ");
            }
        }

        else {
            for (int i = 0; i < hex.length(); i++) {
                newHex.append(hex.charAt(i));
                if (i % 2 == 1) newHex.append(" ");
            }
        }
        return newHex.toString();
    }

    public static int[] floatToBit(double a, int size) {
        boolean negative = a < 0;
        a = Math.abs(a);

        int fractionSize = size == 1 ? 4 : size == 2 ? 7 : size == 3 ? 13 : 19;

        int intPart = (int) (a);
        double fraction = a - intPart;
        int[] intPartArray = integerToBit(String.valueOf(intPart), (int) (Math.log(intPart) / Math.log(2) + 1), false);
        List<Integer> fractionList = fractionToBit(fraction);

        int E = intPartArray.length - 1;
        int bias = (int) Math.pow(2, (size * 8) - fractionSize - 2) - 1;
        int exp = E + bias;


        int[] sum = new int[intPartArray.length + fractionList.size()];
        System.arraycopy(intPartArray, 0, sum, 0, intPartArray.length);
        for (int i = 0; i < fractionList.size(); i++) {
            sum[intPartArray.length + i] = fractionList.get(i);
        }

        int[] roundedSum = new int[fractionSize];
        round(sum, fractionSize);
        //If the first element of the sum array is 0 after the round
        //so the sum array was something like --> 1.111|111
        //It was 0.000 when it was rounded, normally it is 10.000, then it should change to 1.0000, but there is no need to deal with this part.
        //If the first bit is 0 after the round, that means it will shift 1 more comma and get exp + 1
        exp = sum[0] == 0 ? exp + 1: exp;

        int[] expArray = integerToBit(String.valueOf(exp), (int) (Math.log(exp) / Math.log(2) + 1), false);

        for (int i = 1; i <= fractionSize; i++) {
            if (i >= sum.length) roundedSum[i - 1] = 0;
            else roundedSum[i - 1] = sum[i];
        }

        int[] bitLast = new int[1 + expArray.length + roundedSum.length];
        bitLast[0] = negative ? 1 : 0;

        System.arraycopy(expArray, 0, bitLast, 1, expArray.length);

        int index = 0;
        for (int i = expArray.length + 1; i < bitLast.length; i++) {
            bitLast[i] = roundedSum[index++];
        }

        return bitLast;
    }
    
    //round function for fraction part.
    public static void round(int[] sum, int fractionSize) {
        //If the fraction is already as long or shorter as we want, or if the next int is 0, we send it without any rounds.
        //The purpose of the 0 check is if the next int is 0, the roundUp will remain as it is.
        //111|01111111 -> 111
        if (sum.length - 1 <= fractionSize || sum[fractionSize + 1] == 0) return;
        boolean repeat = false;
        
        //First of all, we check if there is another 1 after the first 1, because if there is another 1, we will directly roundUp
        //If there is no we call roundEven 
        //110|10000 -> 110
        //110|10001 -> 111
        for(int i = fractionSize + 2; i < sum.length; i++) {
            if (sum[i] == 1) {
                repeat = true;
                break;
            }
        }
        
        //If we are going to roundEven and the first bit is 0 we send it without doing anything
        if (!repeat && (sum[fractionSize] == 0)) return;
        
        //If not, we turn them all over until we see the first 0. When we see 0, we turn it over and keep
        for (int i = fractionSize; i >= 0; i--) {
            if (sum[i] == 1) sum[i] = 0;
            else {
                sum[i] = 1;
                break;
            }
        }
    }
    
    //Converts the part after the double point to a bit
    public static List<Integer> fractionToBit(double fraction) {
        List<Integer> fractionArray = new ArrayList<>();
        //Why is this function returned 25 times because it can go up to 24 bits at most. If flptsize = 4, we return 1 extra
        //so we know for sure the first bit of where we're going to round
        for (int i = 1; i <= 25; i++) {
            //If the fraction is 0, the remaining elements will already be 0, it does not work for us, so it can stop.
            if (fraction == 0) break;
            
            //it's subtracting 2^-i until then
            if (fraction >= Math.pow(2, -i)) {
                fractionArray.add(1);
                fraction -= Math.pow(2, -i);
            }
            else {
                fractionArray.add(0);
            }
        }
        //Why does this function exist now? Because we need the whole fraction part to know whether to do roundUp or roundEven, but
        //Are we going to loop forever?
        //So if the fraction part is still not 0 after 25 bits, if there are still bits to be added, it doesn't matter where they are.
        //So we can only add one 1 and pass, those parts will be roundUp
        if (fraction != 0) fractionArray.add(1);
        return fractionArray;
    }
}
