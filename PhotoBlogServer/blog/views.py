from django.shortcuts import render
from rest_framework import viewsets
from .models import Post
from .serializers import PostSerializer

class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

def post_list(request):
    posts = Post.objects.all()
    return render(request, 'blog/post_list.html', {'posts': posts})

def js_test(request):
    return render(request, 'blog/js_test.html')