import { Routes } from '@angular/router';
import { SummaryReportComponent } from './components/summary-report/summary-report.component';
import { AuthGuard } from './services/auth.guard';

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
        loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard),
        canActivate: [AuthGuard]
    },

    {
        path: 'tender',
        loadComponent: () => import('./pages/tender/tender').then(m => m.TenderPage),
        canActivate: [AuthGuard]
    },

    {
        path: 'tender/:id',
        loadComponent: () => import('./pages/tender/tender').then(m => m.TenderPage),
        canActivate: [AuthGuard]
    },

    {
        path: 'bidder',
        loadComponent: () => import('./pages/bidder/bidder').then(m => m.Bidder),
        canActivate: [AuthGuard]
    },

    {
        path: 'place-bid/:id',
        loadComponent: () => import('./pages/place-bid/place-bid.component').then(m => m.PlaceBidComponent),
        canActivate: [AuthGuard]
    },

    {
        path: 'tender/:tenderId/bids',
        loadComponent: () => import('./pages/tender-bids/tender-bids-page.component').then(m => m.TenderBidsPageComponent),
        canActivate: [AuthGuard]
    },

    {
        path: 'tender/create',
        loadComponent: () => import('./pages/tender/tender').then(m => m.TenderPage),
        canActivate: [AuthGuard]
    },

    
    {
        path: 'summary-report/bid/:bidId',
        component: SummaryReportComponent,
        canActivate: [AuthGuard]
    },
    {
        path: 'summary-report/tender/:tenderId',
        component: SummaryReportComponent,
        canActivate: [AuthGuard]
    },


    {path: '**', redirectTo: 'login'}
];
