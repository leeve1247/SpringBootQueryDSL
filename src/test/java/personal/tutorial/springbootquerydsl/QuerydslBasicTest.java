package personal.tutorial.springbootquerydsl;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import personal.tutorial.springbootquerydsl.entity.Member;
import personal.tutorial.springbootquerydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static personal.tutorial.springbootquerydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @Autowired
    CriteriaBuilderFactory cbf;
    BlazeJPAQueryFactory blazeQueryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em); // 이건 동시성 문제를 고민하지 않아도 됨, 해결이 됨 spring frame 이 주입해주는 빈 자체가 트랜잭션 에 따라 멀티스레드에서 문제 없이 주입을 해주기 때문에, 큰 문제가 되지 않는다.
        blazeQueryFactory = new BlazeJPAQueryFactory(em, cbf);

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

    @Test
    @DisplayName("멤버 조회 초기 테스트, JPQL")
    public void startJPQL() {
        //member1을 찾아라.
        Member findMember = em.createQuery(
                        "select m from Member m " +
                                "where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("멤버 조회 초기 테스트, QueryDSL")
    public void startQuerydsl() {

        //QMember m1 = new QMember("m3") //딱히 이걸 정해도 값이 바뀌지 않음.. hibernate 버전 업으로 인한 현상인듯 하오 // 별칭이 겹치는 문제를 해결하고자 하는 것인데, 별칭이 안 나뉘네...
        Member findMember = blazeQueryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("멤버 조회, QueryDSL") //원하는 쿼리는 검색하면 다 나옴
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                // .where(member.username.eq("member1").and(member.age.eq(10)))
                .where(member.username.eq("member1"), member.age.eq(10)) //parameter를 여러개 넘길 경우, and 의 형태로, 모든 것을 만족하는 경우에 대한 쿼리를 날린다. 동적 쿼리로서, 조건을 여러개 설정할 때 기가 막히게 처리를 한다.
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // Member fetchOne = queryFactory
        //         .selectFrom(member)
        //         .fetchOne(); // 결과가 둘 이상이면 NonUniqueResultException

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst(); //index = 0 행 하나만 불러오는 기능

        // List<Member> members = queryFactory
        //         .selectFrom(member)
        //         .fetchResults()
        //         .getResults(); //total count를 가져오는 쿼리가 심플해질 정도로 복잡한 쿼리를 호출할 때는 쓰면 안된다.

        List<Member> members = blazeQueryFactory
                .selectFrom(member)
                .orderBy(member.id.asc())
                .fetchPage(0, 2);

        for (Member member : members) {
            System.out.println("member = " + member);
        }

        // long totalQueryDsl = queryFactory
        //         .selectFrom(member)
        //         .fetchCount();

        long totalBlazeQueryDsl = blazeQueryFactory
                .selectFrom(member)
                .fetchCount(); //조회된 member의 수만큼 long type return 해준다.
    }
}
