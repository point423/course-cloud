echo "=== 测试微服务拆分 ==="

# 定义一个函数来美化JSON输出，如果安装了jq的话
function json_pretty {
  if command -v jq &> /dev/null; then
    jq .
  else
    cat # 如果没有jq，就直接输出原始内容
  fi
}

# 1. 测试课程目录服务 - 创建课程
echo -e "\n1. 测试课程目录服务 - 创建课程"
curl -s -X POST http://localhost:8082/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CS101",
    "title": "计算机科学导论",
    "instructor": { "name": "张教授", "email": "zhang@example.edu.cn" },
    "schedule": { "dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "10:00" },
    "capacity": 60,
    "enrolled": 0
  }' || echo "提示: 课程代码已存在或创建失败"
echo ""

# 2. 获取所有课程
echo -e "\n2. 获取所有课程"
curl -s http://localhost:8082/api/courses | json_pretty
echo ""

# 3. 测试选课服务 - 创建学生
echo -e "\n3. 测试选课服务 - 创建学生"
curl -s -X POST http://localhost:8081/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2024001",
    "name": "张三",
    "major": "计算机科学与技术",
    "grade": 2024,
    "email": "zhangsan@example.edu.cn"
  }' || echo "提示: 学号已存在或创建失败"
echo ""

# 4. 获取所有学生
echo -e "\n4. 获取所有学生"
curl -s http://localhost:8081/api/students | json_pretty
echo ""

# 5. 测试学生选课（验证服务间通信）
echo -e "\n5. 测试学生选课"
COURSE_ID=$(curl -s http://localhost:8082/api/courses | jq -r '.[0].id')

if [ -z "$COURSE_ID" ] || [ "$COURSE_ID" == "null" ]; then
    echo "错误：无法获取课程ID！"
else
    echo "获取到课程ID: $COURSE_ID，开始选课..."
    # 使用-i参数显示响应头和响应体
    curl -i -X POST "http://localhost:8083/api/enrollments?courseId=$COURSE_ID&studentId=2024001"
fi
echo ""

# 6. 查询选课记录
echo -e "\n6. 查询选课记录"
curl -s http://localhost:8083/api/enrollments | json_pretty
echo ""

# 7. 测试选课失败（课程不存在）
echo -e "\n7. 测试选课失败（预期返回404 Not Found）"
# 【关键修改】使用 -i 参数来打印完整的HTTP响应，包括状态码和头部信息
curl -i -X POST "http://localhost:8083/api/enrollments?courseId=non-existent-course&studentId=2024001"
echo ""

# 8. 测试选课失败（学生不存在）
echo -e "\n7. 测试选课失败（预期返回404 Not Found）（学生）"
# 【关键修改】使用 -i 参数来打印完整的HTTP响应，包括状态码和头部信息
curl -i -X POST "http://localhost:8083/api/enrollments?courseId=$COURSE_ID&studentId=999999"
echo ""

echo -e "\n=== 测试完成 ==="