package hello.core.singleton;

public class SingletonService {
    private static final SingletonService instance = new SingletonService(); // class 처음 호출 시에만 객체 생성

    public static SingletonService getInstance(){
        return instance;
    }

    private SingletonService(){} //private 생성자로 생성 막기

    public void logic(){
        System.out.println("싱글톤 객체 로직 호출");
    }
}
