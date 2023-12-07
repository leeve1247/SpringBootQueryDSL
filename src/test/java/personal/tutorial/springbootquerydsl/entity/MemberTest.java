package personal.tutorial.springbootquerydsl.entity;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static personal.tutorial.springbootquerydsl.entity.QMember.member;


@SpringBootTest
@Transactional
//@Commit <-- 이게 있으면 Commit 되면서 DB 에 박제가 되는데, 이렇게 되면 다른 테스트와 꼬일 수 있는 문제가 있다.
class MemberTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

//    @Autowired
//    CriteriaBuilderFactory cbf;
//    BlazeJPAQueryFactory blazeQueryFactory;


    @BeforeEach
    public void testEntity(){
        queryFactory = new JPAQueryFactory(em); // 이건 동시성 문제를 고민하지 않아도 됨, 해결됨
//        blazeQueryFactory = new BlazeJPAQueryFactory(em, cbf);

        Team teamA = new Team("teamA");
        Team teamB = new Team("testB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 11, teamA);
        Member member3 = new Member("member3", 12, teamB);
        Member member4 = new Member("member4", 13, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }
}