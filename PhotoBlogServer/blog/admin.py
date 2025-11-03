from django.contrib import admin
from .models import Post

# Post 모델을 admin에 등록
admin.site.register(Post)