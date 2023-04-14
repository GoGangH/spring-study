package hello.core.discount;

import hello.core.annotation.MainDiscountPolicy;
import hello.core.member.Grade;
import hello.core.member.Member;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
//@Primary //여러개의 bean이 있을때 우선 선택
@MainDiscountPolicy
public class RateDiscountPolicy implements DiscountPolicy{

    private int discoutPercent = 10;

    @Override
    public int discount(Member member, int price) {
        if(member.getGrade() == Grade.VIP){
            return price * discoutPercent / 100;
        } else{
            return 0;
        }
    }

}
