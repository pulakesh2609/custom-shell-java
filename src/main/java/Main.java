import java.sql.SQLOutput;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        while(true){

            System.out.print("$ ");
            String Input = scanner.nextLine();
            if(Input.equals ("exit")) {
                break;
            }
            else if(Input.startsWith("echo ")) {
                    System.out.println(Input.substring(5));
                }
            else{
                    System.out.println(Input +": command not found");
                }
        }

    }
}
