package personal.tutorial.springbootquerydsl;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.querydsl.BlazeJPAQuery;
import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.TypedQuery;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import personal.tutorial.springbootquerydsl.dto.MemberDto;
import personal.tutorial.springbootquerydsl.entity.Member;
import personal.tutorial.springbootquerydsl.entity.QMember;
import personal.tutorial.springbootquerydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static personal.tutorial.springbootquerydsl.entity.QMember.member;
import static personal.tutorial.springbootquerydsl.entity.QTeam.team;

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
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
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
                .where(member.username.eq("member1"), member.age.between(10,20)) //parameter를 여러개 넘길 경우, and 의 형태로, 모든 것을 만족하는 경우에 대한 쿼리를 날린다. 동적 쿼리로서, 조건을 여러개 설정할 때 기가 막히게 처리를 한다.
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("멤버 조회, QueryDSL") //원하는 쿼리는 검색하면 다 나옴
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10,20)) //parameter를 여러개 넘길 경우, and 의 형태로, 모든 것을 만족하는 경우에 대한 쿼리를 날린다. 동적 쿼리로서, 조건을 여러개 설정할 때 기가 막히게 처리를 한다.
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst(); //index = 0 행 하나만 불러오는 기능

        long totalBlazeQueryDsl = blazeQueryFactory
                .selectFrom(member)
                .fetchCount(); //조회된 member의 수만큼 long type return 해준다.
        assertThat(totalBlazeQueryDsl).isEqualTo(4);
    }

    @Test
    public void getTotalResults() {
        BlazeJPAQuery<Member> totalMemberQuery = blazeQueryFactory
                .selectFrom(member)
                .orderBy(member.id.asc());
        long fetchCount = totalMemberQuery.fetchCount();
        List<Member> members = totalMemberQuery.fetchPage(0, 20);
        System.out.println("fetchCount = " + fetchCount);
        System.out.println("members = " + members);
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단 2에ㅔ서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    @DisplayName("회원 정렬")
    public void sortMembers() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> sortedMembers = blazeQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = sortedMembers.get(0);
        Member member6 = sortedMembers.get(1);
        Member memberNull = sortedMembers.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    @DisplayName("회원 정렬")
    public void paging1() {
        List<Member> resultList = blazeQueryFactory
                .selectFrom(member)
                .orderBy(member.age.desc())
                .offset(1)
                .limit(2)
                .fetch();
    }

    @Test
    @DisplayName("회원 정렬")
    public void paging2() {
        PagedList<Member> members = blazeQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc(), member.id.asc())
                .offset(1)
                .limit(2)
                .fetchPage(0, 1);
        for (Member member : members) {
            System.out.println("member = " + member);
        }
    }

    @Test
    @DisplayName("집합")
    public void aggregation() {
        List<Tuple> result = blazeQueryFactory //DataType 이 여러개 들어온다 싶으면 tuple을 쓰지만, 실무에선 잘 안쓰고, DTO를 직접 만들어 처리한다.
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령 구하기
     */
    @Test
    @DisplayName("팀 이름과 나이를 찾기")
    public void groupByTeamNameAndAge() throws Exception{
        //given
        List<Tuple> fetch = blazeQueryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * join을 통해 멤버 이름까지 가져오기
     */
    @Test
    @DisplayName("멤버에서 team 이름도 함께 출력하기")
    public void join() {
        List<Member> fetch = blazeQueryFactory
                .selectFrom(member)
                .join(member.team, team) // == innerJoin과 동일함
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    @DisplayName("연관관계 없는 테이블을 조회")
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = blazeQueryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    @DisplayName("조인 온으로 필터링")
    public void join_on() {
        List<Member> result = blazeQueryFactory
                .selectFrom(member)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")) //LeftJoin이 필요할 때만, 조건을 제한하고 싶을 경우에는 on절을 활용할 것. on 절은 필터링 조건을 후행 절에서 파생되는 것
                .fetch();
    }

    @Test
    @DisplayName("연관관계 없는 테이블을 조인하고 조회")
    public void theta_join_on() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> tupleList = blazeQueryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : tupleList) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("페치 조인 없을 때")
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member fetchedOne = blazeQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(fetchedOne.getTeam());
        Assertions.assertThat(loaded).isFalse();
    }

    @Test
    @DisplayName("페치 조인 있을 때") //연관관계가 있을 때, Lazy보다 우선하여 한번에 불러오는 역할을 수행한다.
    public void fetchJoinUsed(){
        em.flush();
        em.clear();

        Member fetchedOne = blazeQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(fetchedOne.getTeam());
        Assertions.assertThat(loaded).isTrue();
    }

    @Test
    @DisplayName("나이 가장 많은 회원 조회") //연관관계가 있을 때, Lazy보다 우선하여 한번에 불러오는 역할을 수행한다.
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = blazeQueryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(40);
    }

    @Test
    @DisplayName("나이 평균 이상 회원 조회") //연관관계가 있을 때, Lazy보다 우선하여 한번에 불러오는 역할을 수행한다.
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = blazeQueryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    @DisplayName("나이 평균 이상 회원 조회") //연관관계가 있을 때, Lazy보다 우선하여 한번에 불러오는 역할을 수행한다.
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = blazeQueryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(20,30, 40);
    }

    @Test //언젠가 해결될거라고 생각하는데.. 해결됐나 이거?
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> tupleList = blazeQueryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : tupleList) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase(){
        List<String> results = queryFactory
                .select(member.age
                        .when(10).then("열살") .when(20).then("스무살") .otherwise("기타"))
                .from(member)
                .fetch();
        for (String result : results) {
            System.out.println("result = " + result);
        }

    }

    @Test
    public void complexCase(){
        List<String> results = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")) //이런거는 그냥 가져와서, 화면 프레젠테이션 레이어에서 처리할 것을 권장
                .from(member)
                .fetch();
        for (String result : results) {
            System.out.println("result = " + result);
        }
    }

    @Test
    public void groupingCase(){
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);
        List<Tuple> result = blazeQueryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = "
                    + rank); }
    }

    @Test
    public void constant() {
        List<Tuple> results = blazeQueryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple result : results) {
            System.out.println("result = " + result);
        }
    }

    @Test
    public void concat(){
        String result = blazeQueryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        System.out.println("result = " + result);
    }

    @Test
    public void tupleProjection() {
        List<Tuple> results = blazeQueryFactory //이걸 리포지토리에서만 쓰고 바깥에서는 DTO로 전환해서 컨트롤러와 서비스로 던질 것을 추천
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple result : results) {
            String username = result.get(member.username);
            Integer age = result.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new personal.tutorial.springbootquerydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    public void findDtoByQueryDSL(){
        List<MemberDto> memberDtoList = blazeQueryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : memberDtoList) {
            System.out.println("memberDto = " + memberDto);
        }
    }
}
