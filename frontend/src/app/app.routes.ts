import { Routes } from '@angular/router';
import { SummaryReportComponent } from './components/summary-report/summary-report.component';

export const routes: Routes = [
    {path: '', redirectTo: 'login', pathMatch: 'full'},

    {
        path: 'login',
        loadComponent: () => import('./pages/login/login').then(m => m.Login)
    },

    {
        path: 'signup',
        loadComponent: () => import('./pages/signup/signup').then(m => m.Signup)
    },

    {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard)
    },

    {
        path: 'tender',
        loadComponent: () => import('./pages/tender/tender').then(m => m.TenderPage)
    },

    {
        path: 'tender/:id',
        loadComponent: () => import('./pages/tender/tender').then(m => m.TenderPage)
    },

    {
        path: 'bidder',
        loadComponent: () => import('./pages/bidder/bidder').then(m => m.Bidder)
    },

    {
        path: 'place-bid/:id',
        loadComponent: () => import('./pages/place-bid/place-bid.component').then(m => m.PlaceBidComponent)
    },

    
    {
        path: 'summary-report/bid/:bidId',
        component: SummaryReportComponent
    },
    {
        path: 'summary-report/tender/:tenderId',
        component: SummaryReportComponent
    },

    // Wildcard route for undefined paths
    {path: '**', redirectTo: 'login'}
];
