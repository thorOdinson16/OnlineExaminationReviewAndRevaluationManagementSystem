// Add these methods to existing RevaluationService.java

    // Get revaluation requests by status (for revaluator)
    public List<RevaluationRequest> getRevaluationsByStatusForRevaluator(String status, Long revaluatorId) {
        return revaluationRepo.findByRevaluatorUserIdAndRevaluationStatus(revaluatorId, status);
    }
    
    // Get pending revaluation requests for revaluator
    public List<RevaluationRequest> getPendingForRevaluator() {
        return revaluationRepo.findPendingForRevaluator();
    }
    
    // Count revaluations by status
    public long countByStatus(String status) {
        return revaluationRepo.countByStatus(status);
    }
    
    // Verify revaluation request (admin)
    @Transactional
    public RevaluationRequest verifyRevaluation(Long revaluationId) {
        return updateRevaluationStatus(revaluationId, "VERIFIED");
    }
    
    // Reject revaluation request
    @Transactional
    public RevaluationRequest rejectRevaluation(Long revaluationId, String reason) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
        
        request.setRevaluationStatus("REJECTED");
        RevaluationRequest savedRequest = revaluationRepo.save(request);
        
        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " has been rejected. Reason: " + reason);
        
        return savedRequest;
    }