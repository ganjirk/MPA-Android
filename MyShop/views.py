from django.http import HttpResponse
from django.shortcuts import render
from .models import Product_List, Offer

# http://127.0.0.1:8000/MyShop will call this
# call index when /mainpage
def index(request):
    myshop = Product_List.objects.all()
    return render(request, 'products.html', {'myshop':myshop})

def new(request):
    return HttpResponse('new products')