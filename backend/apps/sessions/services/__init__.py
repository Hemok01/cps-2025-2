from .recording_analysis_service import RecordingAnalysisService
from .lecture_conversion_service import TaskConversionService

# 기존 호환성 유지
LectureConversionService = TaskConversionService

__all__ = ['RecordingAnalysisService', 'TaskConversionService', 'LectureConversionService']
