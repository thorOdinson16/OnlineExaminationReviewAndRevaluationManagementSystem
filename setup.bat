@echo off
set BASE=src\main\java\com\team\revaluation

mkdir %BASE%\controller
mkdir %BASE%\model
mkdir %BASE%\service
mkdir %BASE%\repository

type nul > %BASE%\controller\StudentController.java
type nul > %BASE%\controller\EvaluatorController.java
type nul > %BASE%\controller\AdminController.java
type nul > %BASE%\controller\RevaluatorController.java

type nul > %BASE%\model\User.java
type nul > %BASE%\model\Student.java
type nul > %BASE%\model\Evaluator.java
type nul > %BASE%\model\Revaluator.java
type nul > %BASE%\model\Admin.java
type nul > %BASE%\model\AnswerScript.java
type nul > %BASE%\model\Exam.java
type nul > %BASE%\model\ReviewRequest.java
type nul > %BASE%\model\RevaluationRequest.java
type nul > %BASE%\model\Payment.java

type nul > %BASE%\service\ReviewService.java
type nul > %BASE%\service\PaymentService.java

type nul > %BASE%\repository\UserRepository.java
type nul > %BASE%\repository\ReviewRequestRepository.java

type nul > %BASE%\RevaluationApplication.java

echo Done! All folders and files created.
pause