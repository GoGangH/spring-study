package com.example.testjpa.start;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.List;

public class Jpamain {

    public static void main(String[] args) {
        //팩토리
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("testjpa");
        //매니저
        EntityManager em = emf.createEntityManager();
        //트랜잭션
        EntityTransaction tx = em.getTransaction();

        try{
            tx.begin(); //트랜잭션 시작
            logic(em);// 비지니스 로직 실행
            tx.commit(); //트랜잭션 커밋
        }catch (Exception e){
            tx.rollback(); //트랜잭션 롤백
        }finally {
            em.close(); // 매니저 종료
        }
        emf.close(); //팩토리 종료
    }

    private static void logic(EntityManager em){
        String id = "id1";
        Member member = new Member();
        member.setId(id);
        member.setUsername("강현");
        member.setAge(26);

        //등록
        em.persist(member);

        //수정
        member.setAge(25);

        //한 건 조회
        Member findMember = em.find(Member.class, id);
        System.out.println("findMember = " + findMember.getUsername() + ", age= " + findMember.getAge());

        //목록 조회
        List<Member> members= em.createQuery("select m from Member m", Member.class).getResultList();
        System.out.println("members.size = " + members.size());

        em.remove(member);
    }
}
