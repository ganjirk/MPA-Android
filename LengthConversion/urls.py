from django.urls import path
from . import views

urlpatterns = [
    path('',views.LenCon,name='LengthConversion')
]