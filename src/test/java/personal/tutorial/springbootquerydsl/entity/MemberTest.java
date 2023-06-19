package personal.tutorial.springbootquerydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static personal.tutorial.springbootquerydsl.entity.QMember.member;


@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    @Autowired
    EntityManagerFactory emf;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void testEntity(){
        queryFactory = new JPAQueryFactory(em); // 이건 동시성 문제를 고민하지 않아도 됨, 해결됨

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
    public void startJPQL() {
        //member1을 찾아라.
        Member findMember = em.createQuery(
                "select m from Member m " +
                        "where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("멤버 조회 초기 테스트, QueryDSL")
    public void startQuerydsl() {

        //QMember m1 = new QMember("m3") //딱히 이걸 정해도 값이 바뀌지 않음.. hibernate 버전 업으로 인한 현상인듯 하오 // 별칭이 겹치는 문제를 해결하고자 하는 것인데, 별칭이 안 나뉘네...
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assert findMember != null;
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
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
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
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
                .fetchFirst(); //맨 앞열 하나만 불러오는 기능

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();//이것에 대한 위험성? query-dsl은 원래 쿼리 내부에 서브쿼리르ㅗ 감싸느 ㄴ형식으로 만드는데,

        List<Member> memberList = results.getResults();
        for (Member member1 : memberList) {
            System.out.println("member1 = " + member1.getUsername());
        }

        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); //select 문들을 전부 Count 로 전환하는 일을 한다.
        // 많은 것들이, deprecated 되어서 활용하기가 곤란하다.
    }
}