# 협업을 위해 git 공부

## Git commond

- branch
- push
- put
- pull
- fetch
- comit
- merge
- tag

## Git flow

### Branch List

- master : 최종 배포 branch
- develop : 배포전 통합 개발 branch
  - feature : 각 기능별 개발 branch
- release : 통합 후 배포전 readme 같은 메타 데이터 수정 branch
- hotfix : 배포 후 이슈 발생시 사용 branch

개발 순서 :

```bash
git checkout develop #develop으로 이동
git branch feature/{기능} # develop에서 feature생성
git checkout develop #다시 develop으로 이동
git merge --no-ff feature/{기능} # feature/{기능}을 develop으로 병합
git branch -d feature/{기능} # merge후 삭제
# develop에 모든 feature 병합 후
git checkout develop #다시 develop으로 이동
git branch release/{버전} #release 생성
# 수정후
git checkout develop
git merge --no-ff release/{버전} #병합
git checkout master # 배포 branch로 이동
git merge --no-ff develop
git tag {tagname} # 배포후 태그로 버전 명세
# 이슈 발생시
git branch hotfix
# 이슈 발생시 release중이면 release에 배포 아니면 develop에 배포
```
