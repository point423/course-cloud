for i in {1..6};
do   echo "--> 发起第 $i 次请求";
curl -X POST "http://localhost:8083/api/enrollments?courseId=60b8b856-f4c6-4cb3-98f0-343c5ff2b7c2&studentId=2024001";
echo "";
sleep 1;
done