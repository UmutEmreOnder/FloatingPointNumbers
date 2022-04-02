package com.company;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("input.txt");
        Scanner scannerFile = new Scanner(file);
        Scanner scannerInput = new Scanner(System.in);
        System.out.print("Byte Ordering: ");
        char byteType = scannerInput.nextLine().charAt(0);
        System.out.print("Floating Point Size: ");
        int flPointSize = scannerInput.nextInt();

        while(scannerFile.hasNext()){
            String number = scannerFile.nextLine();
            int[] answer = number.contains(".") ? floatToBit(Double.parseDouble(number), flPointSize) : integerToBit(number, 16, true);
            String hex = endian(bitToHex(answer), byteType);
            System.out.println(hex);
        }
    }

    public static int stringToInt(String numberString) {
        return numberString.charAt(numberString.length() - 1) == 'u' ? Integer.parseInt(numberString.substring(0, numberString.length() - 1)) : Integer.parseInt(numberString);
    }

    // fixedSize sadece fraction kismindan cagrildiysa fonksiyon false oluyor
    // int signed kisminin amaci 16 bitlik unsigned ya da signed gonderildiginde eger unsigned ise tum bitleri kullanabilir
    // ama signed ise ilk biti signe icin ayirmali o yuzden for dongusu 2. bitten (yani 1. indexten) baslatmali
    // bu dedigim fraction kismindan gelenler icin gecerli olmadigindan fixedSize booleani ekledim
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

    // ilk 1'i gorene kadar bir sey yapmiyor, gordukten sonra her biti ters ceviriyor
    public static void translateToNegative(int[] answer) {
        boolean firstSeen = false;
        for(int i = 15; i >= 0; i--) {
            if (firstSeen) answer[i] = answer[i] == 1 ? 0 : 1;
            if (answer[i] == 1 && !firstSeen) firstSeen = true;
        }
    }

    // ustalik eseri fonksiyonum bi yandan assembly dinlerken bi yandan kodladim anlayan comment ekleyebilir
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

    // endian b ise oldugu gibi birakiyor sadece 2 karakterde bir bosluk ekliyor
    // hoca efendiler bosluksuz input kabul etmedigi icin boyle sikten bir sey yazmak zorunda kaldim
    // l ise de ters ceviriyo iste
    public static String endian(String hex, char endian) {
        StringBuilder newHex = new StringBuilder();

        if (endian == 'b') {
            for (int i = 0; i < hex.length(); i++) {
                newHex.append(hex.charAt(i));
                if (i % 2 == 1) newHex.append(" ");
            }
        }
        else {
            for (int i = hex.length() - 2; i >= 0; i -= 2) {
                newHex.append(hex.charAt(i));
                newHex.append(hex.charAt(i + 1));
                newHex.append(" ");
            }
        }
        return newHex.toString();
    }

    // allah bu fonksiyonu anlamak isteyene sabir versin
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
        int[] expArray = integerToBit(String.valueOf(exp), (int) (Math.log(exp) / Math.log(2) + 1), false);


        int[] sum = new int[intPartArray.length + fractionList.size()];
        System.arraycopy(intPartArray, 0, sum, 0, intPartArray.length);
        for (int i = 0; i < fractionList.size(); i++) {
            sum[intPartArray.length + i] = fractionList.get(i);
        }

        int[] roundedSum = new int[fractionSize];
        round(sum, fractionSize);
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

    // fraction kismi icin round fonksiyonu
    public static void round(int[] sum, int fractionSize) {
        // eger fraction kismi zaten istedigimiz uzunlukta ya da daha kisaysa veya siradaki int 0 ise herhangi bir round yapmadan yolluyoruz
        // 0 kontrolunun amaci eger siradaki int 0sa zaten roundUp yok oldugu gibi kalacak
        // 111|01111111 -> 111
        if (sum.length - 1 <= fractionSize || sum[fractionSize + 1] == 0) return;
        boolean repeat = false;

        // ilk once ilk 1den sonra baska bi 1 var mi diye bakiyoruz, neden? cunku eger baska bi 1 varsa direkt roundUp yapicaz
        // yoook eger yoksa roundEven yapicaz
        // 110|10000 -> 110
        // 110|10001 -> 111
        for(int i = fractionSize + 2; i < sum.length; i++) {
            if (sum[i] == 1) {
                repeat = true;
                break;
            }
        }

        // eger roundEven yapacaksak ve ilk bit 0 ise hicbir sey yapmadan yolluyoz ustte ornegini verdim zaten
        if (!repeat && (sum[fractionSize] == 0)) return;

        // eger degilse de ilk 0'i gorene kadar hepsini ters ceviriyoz 0'i gorunce onu da ters cevirip duruyoz
        for (int i = fractionSize; i >= 0; i--) {
            if (sum[i] == 1) sum[i] = 0;
            else {
                sum[i] = 1;
                break;
            }
        }
    }

    // Guzel optimize edilmis kral fonksiyon, doublein noktadan sonraki kismini bite cevirir
    public static List<Integer> fractionToBit(double fraction) {
        List<Integer> fractionArray = new ArrayList<>();
        // Neden bu fonksiyon 24 kere donuyo gardas? Cunku en fazla 24 bite kadar gidebilir flptsize = 4 ise 1 tane fazladan donduruoz
        // ki roundlayacagimiz yerin ilk bitini kesin olarak bilelim
        for (int i = 1; i <= 25; i++) {
            // eger fraction 0 olduysa zaten kalan elemanlar 0 olacak bi isimize yaramiyo o yuzden durabilir
            if (fraction == 0) break;

            // o zamana kadar 2^-i kadar cikarma yapiyo iste
            if (fraction >= Math.pow(2, -i)) {
                fractionArray.add(1);
                fraction -= Math.pow(2, -i);
            }
            else {
                fractionArray.add(0);
            }
        }
        // isin tatli kismi burasi
        // simdi bura nie var? Cunku roundUp mi roundEven mi yapicaz bilmek icin bize fraction kisminin tamami gerek ama
        // ya 300. bitte 1 tane 1 gelecekse? ya da 3000. bitte? aminakoim sonsuza kadar loop mu dondurecez?
        // o yuzden 25 bitten sonra fraction kismi hala 0 olmadiysa yani hala eklenecek bitler varsa nerede olduklarinin bi onemi yok zaten
        // o yuzden 1 tane 1 ekleyip gecebiliriz o kisimlar roundUp olacak o yuzden umrumuzda degil
        if (fraction != 0) fractionArray.add(1);
        return fractionArray;
    }
}